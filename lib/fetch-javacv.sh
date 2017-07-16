#!/bin/bash
cd "$(dirname "$0")"
#URL=https://search.maven.org/remotecontent?filepath=org/bytedeco/javacv-platform/1.3.2/javacv-platform-1.3.2-bin.zip
#URL=https://search.maven.org/remotecontent?filepath=org/bytedeco/javacv/1.2/javacv-1.2-bin.zip
URL=https://repo1.maven.org/maven2/org/bytedeco/javacv/0.8/javacv-0.8-bin.zip
curl $URL > javacv.zip
mkdir -p javacv-bin/
rm -r javacv-bin/
unzip javacv.zip
