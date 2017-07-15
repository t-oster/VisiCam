#!/bin/bash
sudo docker build -t visicam .
sudo docker run -p 8080:8080 --rm -it visicam
