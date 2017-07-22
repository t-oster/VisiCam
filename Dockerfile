

# This file is a computer- and human-readable description on how to install VisiCam on Ubuntu.
# It is automatically tested to make sure nothing breaks.

FROM ubuntu:17.04
RUN apt-get update

# VisiCam dependencies:
RUN apt-get -y -q --no-install-recommends install openjdk-8-jdk ant libopencv2.4


# not sure for ubuntu 17.10, probably also:
#RUN apt-get -y -q --no-install-recommends install  libopencv-core2.4v5 libopencv-caliv3d2.4v5

# only for testing:
RUN apt-get -y -q --no-install-recommends install curl unzip


# Only for testing: create extra user for VisiCam
RUN adduser --disabled-password visicam
ADD . /home/visicam
RUN chown -R visicam /home/visicam
WORKDIR /home/visicam
USER visicam


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
