mkdir -p "build/tmp/jre"
mkdir "windows-jre"
cd "build/tmp/jre"
wget "https://github.com/ojdkbuild/ojdkbuild/releases/download/1.8.0.141-1/java-1.8.0-openjdk-1.8.0.141-1.b16.ojdkbuild.windows.x86_64.zip" -O "jre.zip"
unzip "jre.zip"
mv java-* extract/
mv extract/jre/ ../../../windows-jre/
