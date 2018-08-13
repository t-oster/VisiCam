

# This file is a computer- and human-readable description on how to install VisiCam on Ubuntu.
# It is automatically tested to make sure nothing breaks.

FROM ubuntu:18.04
RUN apt-get update

# VisiCam dependencies:
RUN apt-get -y -q --no-install-recommends install openjdk-8-jdk ant libopencv2.4 curl unzip


# If you like, create an extra user for VisiCam, and continue as that user
# (optional)
RUN adduser --gecos "" --disabled-password visicam
ADD . /home/visicam
RUN chown -R visicam /home/visicam
WORKDIR /home/visicam
USER visicam


# clone/download the VisiCut git repository and change to that folder (it contains "Dockerfile" and other files)


# Download JavaCV (if it wasn't downloaded yet)
RUN test -d lib/javacv-bin/ || ./lib/fetch-javacv.sh

# Build
RUN ant clean && ant

# Copy test config if no config exists
RUN test -f visicam.conf || cp test/visicam.conf .

# VisiCam will, by default, listen on TCP 8080 and UDP 8888
EXPOSE 8080

# launch via:
CMD ant run
