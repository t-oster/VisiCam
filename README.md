VisiCam
=======

A Java application, that turns a webcam into a [VisiCut](https://visicut.org/) compatible network camera with marker detection and perspective correction. Uses OpenCV through JavaCV

**For detailed information, please have a look at the [VisiCam Wiki](https://github.com/t-oster/VisiCam/wiki).**

Running on Windows
==================

## Exact Requirements
- [Microsoft Visual C++ 2010 Service Pack 1 Redistributable x64](https://www.microsoft.com/de-de/download/details.aspx?id=26999)
- [Apache Ant](https://archive.apache.org/dist/ant/binaries/apache-ant-1.9.9-bin.zip) 1.9.9
- [OpenCV Library](https://sourceforge.net/projects/opencvlibrary/files/opencv-win/2.4.9/opencv-2.4.9.exe/download) 2.4.9
- [JavaCV Library](https://repo1.maven.org/maven2/org/bytedeco/javacv/0.8/javacv-0.8-bin.zip) 0.8
- [Oracle Java JDK](https://download.oracle.com/otn/java/jdk/7u80-b15/jdk-7u80-windows-x64.exe "Oracle Java JDK") 1.7u80 (Login required for downloading)

## Howto compile

* Install JDK
* At first add the `ant` executable to `%PATH%` environment variable
* Add OpenCV bin directory to `%PATH%`: `<location>\opencv\build\x64\vc10\bin`
* Clone VisiCam by `git clone https://github.com/t-oster/VisiCam.git` to desired destination
* Unzip `javacv-0.8-bin.zip` to `<yourdestination>\VisiCam\lib\javacv-bin\`

Then run:
```
cmd
cd "<yourdestination>\VisiCam\"
set "JAVACMD=C:\Program Files\Java\jdk1.7.0_80\bin\java.exe"
ant clean
ant -verbose
```

## Run VisiCam
```
cmd
cd "<yourdestination>\VisiCam\"
ant run
```
You may access the spawned web interface on http://localhost:8080


## Notes
You can also refer to the dedicated [documentation of FabLab Chemnitz](https://wiki.stadtfabrikanten.org/books/laserschneiden-und-gravieren/page/visicam)


Running on Ubuntu
=================

Install the required dependencies with: `sudo apt-get install openjdk-8-jdk ant libopencv2.4`

0. Download JavaCV:
  * On Linux: Simply run `./lib/fetch-javacv.sh` on the command line in the unzipped VisiCam directory.
2. Go in the unzipped VisiCam directory and run `ant` on the command line
3. run `java -jar dist/VisiCam.jar` on the Command Line or double click on the VisiCam.jar file in finder/explorer

Running on Fedora
=================

See https://github.com/t-oster/VisiCam/wiki/Fedora-Installation

Running on the Raspberry Pi
===========================
If you use it on ARM, you need to replace the javacv.jar in the dist/lib folder
with the javacv.jar from the lib/javacv-pi folder.
It works on ArchLinux (opencv can be installed through pacman). But
is VERY SLOW. Any help is appreciated.

TODO: currently broken?

See also https://github.com/t-oster/VisiCam/wiki/Raspberry-Pi-installation-on-Raspbian

Running with Docker
===================
You can also have a look at the [Dockerfile](https://github.com/t-oster/VisiCam/blob/master/Dockerfile) for a more up-to-date list of all commands.

Usage
=====
The following is a short summary of how to use VisiCut. For more detailed information, have a look at the [VisiCam Wiki](https://github.com/t-oster/VisiCam/wiki).

1. Place [4 Markers](https://github.com/t-oster/VisiCam/blob/master/visicam-marker.svg) (Circles within circles) near the corners of you laser-bed.  For a first test, almost any configuration is okay, for example [like this](https://raw.githubusercontent.com/t-oster/VisiCam/master/test/dummy1.jpg). Detailed information on how to achieve a professional set-up can be found in the [VisiCam Wiki](https://github.com/t-oster/VisiCam/wiki).
2. Place a webcam over the laser-cutter, so that it's image contains all the markers.
3. Connect the webcam to a PC running VisiCam.
4. Go to you web browser and enter the VisiCam URL (is shown in the window after starting VisiCam).
5. Click on "Refresh" on the left side to check if the camera is working and the markers are visible.
6. Click on "Show Configuration". Here you can specify the resolutions and more important:
7. For each Marker-Position (top-left, top-right, bottom-left, bottom-right) select a rectangle on the image, where VisiCam should search the marker
8. Save the configuration with a click on the save-button
9. In VisiCut go to `Preferences -> Manage Lasercutters -> Edit` and enter the CameraURL, which is `<VisiCam URL>/image`. If you have the latest version, you can just click "search" and all VisiCam instances in your Network should appear.
10. You should see now the image from the webcam. To calibrate the camera go in Visicut to `Options -> Calibrate Camera...` and choose a laser setting. Then click on the `Send Calibration Page`-Button. VisiCut will send a calibration file to your lasercutter. It will cut two crosses (with the choosen settings) at (20%/20%) and (80%/80%) of the laser-bed. In the following dialog, you have to take a picture and move the red reference crosses matching to the ones you did just cut.
11. Be happy with your VisiCam ;)
