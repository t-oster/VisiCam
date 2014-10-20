VisiCam
=======

A Java application, that turns a webcam into a VisiCut compatible network camera with marker detection and perspective correction. Uses OpenCV through JavaCV

Compile and Run
===============
1. Make sure you have apache-ant, java jdk >=6 and openCv installed
2. run "ant jar" on the command line
3. run java -jar dist/VisiCam.jar on the Command Line or double click in finder/explorer

Running on the Raspberry Pi
===========================
If you use it on ARM, you need to replace the javacv.jar in the dist/lib folder
with the javacv.jar from the lib/javacv-pi folder.
It works on ArchLinux (opencv can be installed through pacman). But
is VERY SLOW. Any help is appreciated.

Running on Windows
==================
- Download and install 
    Microsoft Visual C++ 2010 Redistributable Package (x86) http://www.microsoft.com/download/en/details.aspx?id=5555
  or
    Microsoft Visual C++ 2010 Redistributable Package (x64) http://www.microsoft.com/download/en/details.aspx?id=14632

- Download OpenCV from http://sourceforge.net/projects/opencvlibrary/files/opencv-win/2.4.3/OpenCV-2.4.3.exe/download
- Add either the build/vc10/bin folder to PATH or copy all the dlls to the VisiCam folder

Running on Ubuntu
=================

sudo apt-get install libopencv2.4-java # TODO add java
TODO

Usage
=====
1. Place 4 Markers (Circles within circles) at the corners of you laser-bed
2. Place a webcam over the laser-cutter, so that it's image contains all the markers
3. Connect the webcam to a PC running VisiCam
4. Go to you webbrowser and enter the VisiCam URL (is printed on the Command Line after starting)
5. Click on "Get image from your webcam" to check if the camera is working and the markers are visible
6. Click on "Show Configuration". Here you can specify the resolutions and more important:
7. For each Marker-Position (top-left...) select a rectangle on the image, where VisiCam should search the marker
8. Save the configuration
9. In VisiCut go to Preferences->Manage Lasercutters->Edit and enter the CameraURL, which is the VisiCam URL with /image at the end. If you have the latest version, you can just click "search" and all VisiCam instances in your Network should appear.
