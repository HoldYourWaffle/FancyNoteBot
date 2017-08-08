#!/bin/bash
set -e # script fails when a single command fails
#$scriptdir="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
scriptdir="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd $scriptdir # change into cross-compiling directory

# DOWNLOADING LAUNCH4J
echo "Downloading Launch4j..."
rm -fr "launch4j" # with the -f flag it won't fail when there is no "launch4j" folder (AKA no launch4j-dist has ever been downloaded)
mkdir -p "../build/tmp/launch4j" && cd "$_" # create temporary extraction folder in build directory. This way it'll get cleaned up automatically with gradle clean
wget "https://downloads.sourceforge.net/project/launch4j/launch4j-3/3.11/launch4j-3.11-linux-x64.tgz?r=https%3A%2F%2Fsourceforge.net%2Fprojects%2Flaunch4j%2Ffiles%2Flaunch4j-3%2F3.11%2F&ts=1501782812&use_mirror=netcologne" -nv -O "launch4j.tgz"
echo "Extracting Launch4j..."
tar xf "launch4j.tgz"
mv "launch4j/" "$scriptdir/" # move extracted launch4j distro into the cross-compiling directory

cd $scriptdir # reset for convenience

# DOWNLOADING WINDOWS-JRE
echo "Downloading Windows JRE..."
rm -fr "windows-jre"
mkdir -p "../build/tmp/jre" && cd "$_" # another temp extraction folder
wget "https://github.com/ojdkbuild/ojdkbuild/releases/download/1.8.0.141-1/java-1.8.0-openjdk-1.8.0.141-1.b16.ojdkbuild.windows.x86_64.zip" -nv -O "jre.zip" # download community build openjdk-distributions
echo "Extracting Windows JRE..."
unzip -q "jre.zip"
mv java-* extract/ # rename version-independent directory into a uniform directory to prevent confusion
mv extract/jre/ "$scriptdir/windows-jre/"

cd $scriptdir # reset again

# DOWNLOADING INNO-SETUP
echo "Downloading Inno-Setup..."
rm -fr "inno-setup"
mkdir -p "../build/tmp/inno-setup" && cd "$_" # more temps
wget "http://www.jrsoftware.org/download.php/is.exe" -nv -O "setup.exe" # download setup program (I think this dynamically downloads the latest version?)
echo "================== THIS IS VERY VERY IMPORTANT =================="
echo "Next up the Inno-Setup setup program will be run. You MUST set the installation directory to this exact location:"
echo "$scriptdir/inno-setup"
echo "===================== END OF VERY IMPORTANT ===================="
read -p "Press [Enter] to continue"
wine "setup.exe"

cd $scriptdir # reset

# DOWNLOADING ALZIP
echo "Downloading ALZip..."
rm -fr "alzip"
mkdir -p "../build/tmp/alzip" && cd "$_" # temp
wget "http://advert.estsoft.com/?event=201010121143876" -nv -O "alzip.exe" # download alzip
echo "================== THIS IS VERY VERY IMPORTANT =================="
echo "Next up the Inno-Setup setup program will be run. You MUST set the installation directory to this exact location:"
echo "$scriptdir/alzip"
echo "===================== END OF VERY IMPORTANT ===================="
read -p "Press [Enter] to continue"
wine "alzip.exe"