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
package org.gradle.api.file;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Information about a file in a {@link FileTree}.
 * 一个文件在文件树中的信息
 */
public interface FileTreeElement {
    /**
     * Returns the file being visited.
     *
     * @return The file. Never returns null.
     * 该文件
     */
    File getFile();

    /**
     * Returns true if this element is a directory, or false if this element is a regular file.
     *
     * @return true if this element is a directory.
     * 该文件是否是目录
     */
    boolean isDirectory();

    /**
     * Returns the last modified time of this file at the time of file traversal.
     *
     * @return The last modified time.
     * 文件遍历中最后的修改时间
     */
    long getLastModified();

    /**
     * Returns the size of this file at the time of file traversal.
     *
     * @return The size, in bytes.
     * 文件遍历时该文件的大学
     */
    long getSize();

    /**
     * Opens this file as an input stream. Generally, calling this method is more performant than calling {@code new
     * FileInputStream(getFile())}.
     *
     * @return The input stream. Never returns null. The caller is responsible for closing this stream.
     * 打开文件的输入流，比普通的 FileInputStream(getFile()) 更高效
     */
    InputStream open();

    /**
     * Copies the content of this file to an output stream. Generally, calling this method is more performant than
     * calling {@code new FileInputStream(getFile())}.
     *
     * @param output The output stream to write to. The caller is responsible for closing this stream.
     * 拷贝文件内容到 output，比new FileInputStream(getFile())更高效
     */
    void copyTo(OutputStream output);

    /**
     * Copies this file to the given target file. Does not copy the file if the target is already a copy of this file.
     *
     * @param target the target file.
     * @return true if this file was copied, false if it was up-to-date
     * 拷贝文件内容到 target，如果目标文件已经是该文件的拷贝，则不会进行实际的拷贝
     */
    boolean copyTo(File target);

    /**
     * Returns the base name of this file.
     *
     * @return The name. Never returns null.
     * 文件 base name
     */
    String getName();

    /**
     * Returns the path of this file, relative to the root of the containing file tree. Always uses '/' as the hierarchy
     * separator, regardless of platform file separator. Same as calling <code>getRelativePath().getPathString()</code>.
     *
     * @return The path. Never returns null.
     * 返回 基于文件树的相对路径。 永远以 '/' 做路径分割符。 与 getRelativePath().getPathString() 类似
     */
    String getPath();

    /**
     * Returns the path of this file, relative to the root of the containing file tree.
     *
     * @return The path. Never returns null.
     * 返回 基于文件树的相对路径
     */
    RelativePath getRelativePath();

    /**
     * Returns the Unix permissions of this file, e.g. {@code 0644}.
     *
     * @return The Unix file permissions.
     * 该文件在 unix 上的权限
     */
    int getMode();
}
