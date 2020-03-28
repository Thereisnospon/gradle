
homeDir=`echo ~`
buildDir=$homeDir/.sourceGradle/build
installDir=$buildDir/sgradle5


mkdir -p $buildDir
./gradlew install -Pgradle_installPath=$installDir

echo $installDir
