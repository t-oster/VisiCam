#!/bin/bash
set -e
# Run this script, then the visicam server will be opened on port 8080.
sudo docker build -t visicam .
sudo docker run -p 8080:8080 --rm -it visicam
