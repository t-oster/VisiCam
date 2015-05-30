#!/bin/bash

#    This file is part of VisiCam. (https://github.com/t-oster/VisiCam)
#
#    VisiCam is free software: you can redistribute it and/or modify
#    it under the terms of the GNU Lesser General Public License as published by
#    the Free Software Foundation, either version 3 of the License, or
#    (at your option) any later version.
#
#    VisiCam is distributed in the hope that it will be useful,
#    but WITHOUT ANY WARRANTY; without even the implied warranty of
#    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
#    GNU General Public License for more details.
#
#    You should have received a copy of the GNU Lesser General Public License
#    along with VisiCam.  If not, see <http://www.gnu.org/licenses/>.

# ##################################################################
# FUNCTIONS
# ##################################################################

# Argument 1: Source path to binaries, which need to be copied, without ending slash
# Argument 2: Filename to copy from source directory to dist/lib/
function check_copy_javacv_file
{
    # Does file exist
    if [ -e "$1/$2" ]
    then
        cp "$1/$2" "dist/lib/$2"

        # Check if copy was successful
        if [ $? -ne 0 ]
        then
            echo "[WARNING] Could not copy $1/$2 file to dist/lib/."
            echo "[WARNING] VisiCam might get runtime errors!"
        else
            echo "[INFO] Copied file $1/$2 successfully to dist/lib/!"
        fi
    else
        echo "[WARNING] Could not find $1/$2 file."
        echo "[WARNING] VisiCam might get runtime errors!"
    fi

    return 0
}

# ##################################################################
# MAIN
# ##################################################################

# Change working directory to local directory
cd "$(dirname $0)"

# Compile VisiCam
ant jar

# Check if compile was successful
if [ $? -ne 0 ]
then
    echo "[ERROR] Compilation error!"
    exit 1
fi

# Ask for javacv path
echo ""
echo "[INFO] Please enter the full absolute path to javacv main directory without ending slash."
echo "[INFO] Self-compiled binaries for javacv are recommended."
echo "[INFO] You can leave this empty, then precompiled javacv binaries for Raspbian are used."
echo "[INFO] Example: /home/pi/javacv"
read -p "Path: " JAVACVPATH
echo ""

# Check javacv path and files
if [ -d "$JAVACVPATH" ]
then
    # Try to copy them to dist folder
    check_copy_javacv_file "$JAVACVPATH/target" "javacpp.jar"
    check_copy_javacv_file "$JAVACVPATH/target" "javacv.jar"
    check_copy_javacv_file "$JAVACVPATH/target" "javacv-linux-arm.jar"
else
    echo "[INFO] Fallback to precompiled javacv binaries for Raspbian!"

    # Try to copy them to dist folder
    check_copy_javacv_file "lib/javacv-pi" "javacpp.jar"
    check_copy_javacv_file "lib/javacv-pi" "javacv.jar"
    check_copy_javacv_file "lib/javacv-pi" "javacv-linux-arm.jar"
fi