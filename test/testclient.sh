#!/bin/bash
set -e
curl -v http://localhost:8080/image > client.jpg
find client.jpg -size +100k -ls || { echo "file too small"; exit 1; }



