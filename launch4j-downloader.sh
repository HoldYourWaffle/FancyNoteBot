mkdir -p "build/tmp/launch4j"
rm -r "launch4j"
cd "build/tmp/launch4j"
wget "https://downloads.sourceforge.net/project/launch4j/launch4j-3/3.11/launch4j-3.11-linux-x64.tgz?r=https%3A%2F%2Fsourceforge.net%2Fprojects%2Flaunch4j%2Ffiles%2Flaunch4j-3%2F3.11%2F&ts=1501782812&use_mirror=netcologne" -O "launch4j.tgz"
tar xf "launch4j.tgz"
mv launch4j ../../../