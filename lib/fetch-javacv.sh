#!/bin/sh
curl https://search.maven.org/remotecontent?filepath=org/bytedeco/javacv-platform/1.3.2/javacv-platform-1.3.2-bin.zip > javacv.zip
rm -r javacv-bin/
unzip javacv.zip
