#!/bin/sh
cd $(dirname $0)
sleep 0.4
touch dummyCapture.jpg
cmp dummyCapture.jpg dummy1.jpg && cp dummy2.jpg dummyCapture.jpg && exit 0
cmp dummyCapture.jpg dummy2.jpg && cp dummy3.jpg dummyCapture.jpg && exit 0
cmp dummyCapture.jpg dummy3.jpg && cp dummy4.jpg dummyCapture.jpg && exit 0
cp dummy1.jpg dummyCapture.jpg

