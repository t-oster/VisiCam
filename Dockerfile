FROM ubuntu:16.04
RUN apt-get update
RUN apt-get -y -q --no-install-recommends install openjdk-8-jdk ant libopencv2.4
RUN apt-get -y -q --no-install-recommends install curl unzip
RUN adduser --disabled-password visicam
ADD . /home/visicam
RUN chown -R visicam /home/visicam
WORKDIR /home/visicam
USER visicam
# fetch javaCV if it wasn't downloaded yet
RUN test -d lib/javacv-bin/ || ./lib/fetch-javacv.sh
RUN ant clean && ant
EXPOSE 8080
CMD ant run
