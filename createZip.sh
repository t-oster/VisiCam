#!/bin/bash
# Creates a Zip file ready to publish
version=$(git describe --tags --dirty --always)
rm -rf dist/*
ant jar
cp README.md LICENSE COPYING.LESSER dist/
cp -r html dist/
cd dist
zip -r ../VisiCam-$version.zip .