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

# Argument 1: filename to copy from javacv/target/ to dist/lib/
function check_copy_javacv_file
{
    # Does file exist
    if [ -e "$JAVACVPATH/target/$1" ]
    then
        cp "$JAVACVPATH/target/$1" "dist/lib/$1"

        # Check if copy was succesful
        if [ $? -ne 0 ]
        then
            echo "[WARNING] Could not copy $1 file to dist/lib/."
            echo "[WARNING] VisiCam might get runtime errors!"
        fi
    else
        echo "[WARNING] Could not find $1 file."
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

# Check if compile was succesful
if [ $? -ne 0 ]
then
    echo "[ERROR] Compilation error!"
    exit 1
fi

# Ask for javacv path
echo ""
echo "[INFO] Please enter the full absolute path to javacv main directory without ending slash."
echo "[INFO] Example: /home/pi/javacv"
read -p "Path: " JAVACVPATH
echo ""

# Check javacv path and files
# Copy them to dist folder
if [ -d "$JAVACVPATH" ]
then
    check_copy_javacv_file "javacpp.jar"
    check_copy_javacv_file "javacv.jar"
    check_copy_javacv_file "javacv-linux-arm.jar"
else
    echo "[WARNING] Directory $JAVACVPATH does not exist!"
    echo "[WARNING] VisiCam might get runtime errors!"
fi