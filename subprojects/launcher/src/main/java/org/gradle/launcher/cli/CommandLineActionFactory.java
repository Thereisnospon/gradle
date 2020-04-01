/*
 * Copyright 2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gradle.launcher.cli;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.tools.ant.Main;
import org.codehaus.groovy.util.ReleaseInfo;
import org.gradle.api.Action;
import org.gradle.api.internal.file.IdentityFileResolver;
import org.gradle.api.logging.configuration.LoggingConfiguration;
import org.gradle.cli.CommandLineArgumentException;
import org.gradle.cli.CommandLineConverter;
import org.gradle.cli.CommandLineParser;
import org.gradle.cli.ParsedCommandLine;
import org.gradle.cli.SystemPropertiesCommandLineConverter;
import org.gradle.concurrent.ParallelismConfiguration;
import org.gradle.configuration.GradleLauncherMetaData;
import org.gradle.initialization.BuildLayoutParameters;
import org.gradle.initialization.LayoutCommandLineConverter;
import org.gradle.initialization.ParallelismConfigurationCommandLineConverter;
import org.gradle.initialization.layout.BuildLayoutFactory;
import org.gradle.internal.Actions;
import org.gradle.internal.IoActions;
import org.gradle.internal.buildevents.BuildExceptionReporter;
import org.gradle.internal.concurrent.DefaultParallelismConfiguration;
import org.gradle.internal.jvm.Jvm;
import org.gradle.internal.jvm.inspection.CachingJvmVersionDetector;
import org.gradle.internal.jvm.inspection.DefaultJvmVersionDetector;
import org.gradle.internal.logging.DefaultLoggingConfiguration;
import org.gradle.internal.logging.LoggingCommandLineConverter;
import org.gradle.internal.logging.LoggingManagerInternal;
import org.gradle.internal.logging.services.LoggingServiceRegistry;
import org.gradle.internal.logging.text.StyledTextOutputFactory;
import org.gradle.internal.nativeintegration.services.NativeServices;
import org.gradle.internal.os.OperatingSystem;
import org.gradle.internal.service.ServiceRegistry;
import org.gradle.launcher.bootstrap.ExecutionListener;
import org.gradle.launcher.cli.converter.LayoutToPropertiesConverter;
import org.gradle.launcher.cli.converter.PropertiesToLogLevelConfigurationConverter;
import org.gradle.launcher.cli.converter.PropertiesToParallelismConfigurationConverter;
import org.gradle.process.internal.DefaultExecActionFactory;
import org.gradle.util.GFileUtils;
import org.gradle.util.GradleVersion;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>Responsible for converting a set of command-line arguments into a {@link Runnable} action.</p>
 */
public class CommandLineActionFactory {
    public static final String WELCOME_MESSAGE_ENABLED_SYSTEM_PROPERTY = "org.gradle.internal.launcher.welcomeMessageEnabled";
    private static final String HELP = "h";
    private static final String VERSION = "v";

    private final BuildLayoutFactory buildLayoutFactory = new BuildLayoutFactory();

    /**
     * <p>Converts the given command-line arguments to an {@link Action} which performs the action requested by the
     * command-line args.
     *
     * @param args The command-line arguments.
     * @return The action to execute.
     */
    public Action<ExecutionListener> convert(List<String> args) {
        //先配置好 log 服务
        ServiceRegistry loggingServices = createLoggingServices();

        LoggingConfiguration loggingConfiguration = new DefaultLoggingConfiguration();

        return new WithLogging(loggingServices,
            buildLayoutFactory,
            args,
            loggingConfiguration,
            //真正的命令行解析和 Build  动作
            new ParseAndBuildAction(loggingServices, args),
            //报告 build 错误
            new BuildExceptionReporter(loggingServices.get(StyledTextOutputFactory.class), loggingConfiguration, clientMetaData()));
    }

    protected void createActionFactories(ServiceRegistry loggingServices, Collection<CommandLineAction> actions) {
        actions.add(new BuildActionsFactory(loggingServices, new ParametersConverter(buildLayoutFactory), new CachingJvmVersionDetector(new DefaultJvmVersionDetector(new DefaultExecActionFactory(new IdentityFileResolver())))));
    }

    private static GradleLauncherMetaData clientMetaData() {
        return new GradleLauncherMetaData();
    }

    public ServiceRegistry createLoggingServices() {
        return LoggingServiceRegistry.newCommandLineProcessLogging();
    }

    private static void showUsage(PrintStream out, CommandLineParser parser) {
        out.println();
        out.print("USAGE: ");
        clientMetaData().describeCommand(out, "[option...]", "[task...]");
        out.println();
        out.println();
        parser.printUsage(out);
        out.println();
    }

    static class WelcomeMessageAction implements Action<PrintStream> {
        private final BuildLayoutParameters buildLayoutParameters;
        private final GradleVersion gradleVersion;
        private final Function<String, InputStream> inputStreamProvider;

        WelcomeMessageAction(BuildLayoutParameters buildLayoutParameters) {
            this(buildLayoutParameters, GradleVersion.current(), new Function<String, InputStream>() {
                @Nullable
                @Override
                public InputStream apply(@Nullable String input) {
                    return getClass().getClassLoader().getResourceAsStream(input);
                }
            });
        }

        @VisibleForTesting
        WelcomeMessageAction(BuildLayoutParameters buildLayoutParameters, GradleVersion gradleVersion, Function<String, InputStream> inputStreamProvider) {
            this.buildLayoutParameters = buildLayoutParameters;
            this.gradleVersion = gradleVersion;
            this.inputStreamProvider = inputStreamProvider;
        }

        @Override
        public void execute(PrintStream out) {
            if (isWelcomeMessageEnabled()) {
                File markerFile = getMarkerFile();

                if (!markerFile.exists()) {
                    out.println();
                    out.print("Welcome to Gradle " + gradleVersion.getVersion() + "!");

                    String featureList = readReleaseFeatures();

                    if (StringUtils.isNotBlank(featureList)) {
                        out.println();
                        out.println();
                        out.println("Here are the highlights of this release:");
                        out.print(featureList);
                    }

                    if (!gradleVersion.isSnapshot()) {
                        out.println();
                        out.println("For more details see https://docs.gradle.org/" + gradleVersion.getVersion() + "/release-notes.html");
                    }

                    out.println();

                    writeMarkerFile(markerFile);
                }
            }
        }

        /**
         * The system property is set for the purpose of internal testing.
         * In user environments the system property will never be available.
         */
        private boolean isWelcomeMessageEnabled() {
            String messageEnabled = System.getProperty(WELCOME_MESSAGE_ENABLED_SYSTEM_PROPERTY);

            if (messageEnabled == null) {
                return true;
            }

            return Boolean.parseBoolean(messageEnabled);
        }

        private File getMarkerFile() {
            File gradleUserHomeDir = buildLayoutParameters.getGradleUserHomeDir();
            File notificationsDir = new File(gradleUserHomeDir, "notifications");
            File versionedNotificationsDir = new File(notificationsDir, gradleVersion.getVersion());
            return new File(versionedNotificationsDir, "release-features.rendered");
        }

        private String readReleaseFeatures() {
            InputStream inputStream = inputStreamProvider.apply("release-features.txt");

            if (inputStream != null) {
                StringWriter writer = new StringWriter();

                try {
                    IOUtils.copy(inputStream, writer, "UTF-8");
                    return writer.toString();
                } catch (IOException e) {
                    // do not fail the build as feature is non-critical
                } finally {
                    IoActions.closeQuietly(inputStream);
                }
            }

            return null;
        }

        private void writeMarkerFile(File markerFile) {
            GFileUtils.mkdirs(markerFile.getParentFile());
            GFileUtils.touch(markerFile);
        }
    }

    private static class BuiltInActions implements CommandLineAction {
        public void configureCommandLineParser(CommandLineParser parser) {
            parser.option(HELP, "?", "help").hasDescription("Shows this help message.");
            parser.option(VERSION, "version").hasDescription("Print version info.");
        }

        public Runnable createAction(CommandLineParser parser, ParsedCommandLine commandLine) {
            if (commandLine.hasOption(HELP)) {
                return new ShowUsageAction(parser);
            }
            if (commandLine.hasOption(VERSION)) {
                return new ShowVersionAction();
            }
            return null;
        }
    }

    private static class CommandLineParseFailureAction implements Action<ExecutionListener> {
        private final Exception e;
        private final CommandLineParser parser;

        public CommandLineParseFailureAction(CommandLineParser parser, Exception e) {
            this.parser = parser;
            this.e = e;
        }

        public void execute(ExecutionListener executionListener) {
            System.err.println();
            System.err.println(e.getMessage());
            showUsage(System.err, parser);
            executionListener.onFailure(e);
        }
    }

    private static class ShowUsageAction implements Runnable {
        private final CommandLineParser parser;

        public ShowUsageAction(CommandLineParser parser) {
            this.parser = parser;
        }

        public void run() {
            showUsage(System.out, parser);
        }
    }

    private static class ShowVersionAction implements Runnable {
        public void run() {
            GradleVersion currentVersion = GradleVersion.current();
            KotlinDslVersion currentKotlinDslVersion = KotlinDslVersion.current();

            final StringBuilder sb = new StringBuilder();
            sb.append("%n------------------------------------------------------------%nGradle ");
            sb.append(currentVersion.getVersion());
            sb.append("%n------------------------------------------------------------%n%nBuild time:   ");
            sb.append(currentVersion.getBuildTime());
            sb.append("%nRevision:     ");
            sb.append(currentVersion.getRevision());
            sb.append("%n%nKotlin DSL:   ");
            sb.append(currentKotlinDslVersion.getProviderVersion());
            sb.append("%nKotlin:       ");
            sb.append(currentKotlinDslVersion.getKotlinVersion());
            sb.append("%nGroovy:       ");
            sb.append(ReleaseInfo.getVersion());
            sb.append("%nAnt:          ");
            sb.append(Main.getAntVersion());
            sb.append("%nJVM:          ");
            sb.append(Jvm.current());
            sb.append("%nOS:           ");
            sb.append(OperatingSystem.current());
            sb.append("%n");

            System.out.println(String.format(sb.toString()));
        }
    }

    private static class WithLogging implements Action<ExecutionListener> {
        private final ServiceRegistry loggingServices;
        private final BuildLayoutFactory buildLayoutFactory;
        private final List<String> args;
        private final LoggingConfiguration loggingConfiguration;
        private final Action<ExecutionListener> action;
        private final Action<Throwable> reporter;

        WithLogging(ServiceRegistry loggingServices, BuildLayoutFactory buildLayoutFactory, List<String> args, LoggingConfiguration loggingConfiguration, Action<ExecutionListener> action, Action<Throwable> reporter) {
            this.loggingServices = loggingServices;
            this.buildLayoutFactory = buildLayoutFactory;
            this.args = args;
            this.loggingConfiguration = loggingConfiguration;
            this.action = action;
            this.reporter = reporter;
        }
        // 包装了真正的 parse and build action
        public void execute(ExecutionListener executionListener) {
            //log 级别等 参数解析
            CommandLineConverter<LoggingConfiguration> loggingConfigurationConverter = new LoggingCommandLineConverter();
            //gradle-user-home , project-dir 等参数解析
            CommandLineConverter<BuildLayoutParameters> buildLayoutConverter = new LayoutCommandLineConverter();
            //org.gradle.parallel , max-workers 等参数解析
            CommandLineConverter<ParallelismConfiguration> parallelConverter = new ParallelismConfigurationCommandLineConverter();
            //-D systemProperties 参数解析
            CommandLineConverter<Map<String, String>> systemPropertiesCommandLineConverter = new SystemPropertiesCommandLineConverter();
            LayoutToPropertiesConverter layoutToPropertiesConverter = new LayoutToPropertiesConverter(buildLayoutFactory);

            BuildLayoutParameters buildLayout = new BuildLayoutParameters();
            ParallelismConfiguration parallelismConfiguration = new DefaultParallelismConfiguration();

            CommandLineParser parser = new CommandLineParser();
            loggingConfigurationConverter.configure(parser);
            buildLayoutConverter.configure(parser);
            parallelConverter.configure(parser);
            systemPropertiesCommandLineConverter.configure(parser);

            parser.allowUnknownOptions();
            parser.allowMixedSubcommandsAndOptions();

            try {
                //先解析命令行参数
                ParsedCommandLine parsedCommandLine = parser.parse(args);
                //解析 gradle-user-home , project-dir 等
                buildLayoutConverter.convert(parsedCommandLine, buildLayout);


                Map<String, String> properties = new HashMap<String, String>();
                // Read *.properties files
                //根据之前 buildLayout 解析到的 project-dir，从中读取 gradle.properties 等到 properties 中
                layoutToPropertiesConverter.convert(buildLayout, properties);
                // Read -D command line flags
                //解析 -D 命令行参数到 properties 中
                systemPropertiesCommandLineConverter.convert(parsedCommandLine, properties);

                // Convert properties for logging  object
                //properties 转换到 log 中
                PropertiesToLogLevelConfigurationConverter propertiesToLogLevelConfigurationConverter = new PropertiesToLogLevelConfigurationConverter();
                propertiesToLogLevelConfigurationConverter.convert(properties, loggingConfiguration);
                loggingConfigurationConverter.convert(parsedCommandLine, loggingConfiguration);

                // Convert properties to ParallelismConfiguration object
                PropertiesToParallelismConfigurationConverter propertiesToParallelismConfigurationConverter = new PropertiesToParallelismConfigurationConverter();
                propertiesToParallelismConfigurationConverter.convert(properties, parallelismConfiguration);
                // Parse parallelism flags
                parallelConverter.convert(parsedCommandLine, parallelismConfiguration);
            } catch (CommandLineArgumentException e) {
                // Ignore, deal with this problem later
                //这里只是先简单的扫一遍参数，真正的 执行，错误处理需要在后面进行
            }

            LoggingManagerInternal loggingManager = loggingServices.getFactory(LoggingManagerInternal.class).create();
            //根据第一步命令行参数得到的 loglevel
            loggingManager.setLevelInternal(loggingConfiguration.getLogLevel());
            loggingManager.start();
            //action 为原始的 parse and build action, reporter 为原始的 build 错误 reporter
            Action<ExecutionListener> exceptionReportingAction = new ExceptionReportingAction(action, reporter, loggingManager);
            try {
                //配置native相关？
                NativeServices.initialize(buildLayout.getGradleUserHomeDir());
                //配置log显示到控制台相关逻辑?
                loggingManager.attachProcessConsole(loggingConfiguration.getConsoleOutput());
                //gradle 欢迎信息(版本首次安装时?)
                new WelcomeMessageAction(buildLayout).execute(System.out);
                //真正执行 parse and build action
                exceptionReportingAction.execute(executionListener);
            } finally {
                loggingManager.stop();
            }
        }
    }
    //初步解析命令行参数之后真正执行的 解析和 Build 操作
    private class ParseAndBuildAction implements Action<ExecutionListener> {
        private final ServiceRegistry loggingServices;
        private final List<String> args;

        private ParseAndBuildAction(ServiceRegistry loggingServices, List<String> args) {
            this.loggingServices = loggingServices;
            this.args = args;
        }

        public void execute(ExecutionListener executionListener) {
            List<CommandLineAction> actions = new ArrayList<CommandLineAction>();
            //help, version
            actions.add(new BuiltInActions());
            //解析系统参数，命令行参数等
            createActionFactories(loggingServices, actions);

            CommandLineParser parser = new CommandLineParser();
            for (CommandLineAction action : actions) {
                action.configureCommandLineParser(parser);
            }

            Action<? super ExecutionListener> action;
            try {
                ParsedCommandLine commandLine = parser.parse(args);
                action = createAction(actions, parser, commandLine);
            } catch (CommandLineArgumentException e) {
                action = new CommandLineParseFailureAction(parser, e);
            }

            action.execute(executionListener);
        }

        private Action<? super ExecutionListener> createAction(Iterable<CommandLineAction> factories, CommandLineParser parser, ParsedCommandLine commandLine) {
            for (CommandLineAction factory : factories) {
                Runnable action = factory.createAction(parser, commandLine);
                if (action != null) {
                    return Actions.toAction(action);
                }
            }
            throw new UnsupportedOperationException("No action factory for specified command-line arguments.");
        }
    }
}
