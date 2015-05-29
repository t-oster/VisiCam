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
# ROOT
# ##################################################################

# Check if script is executed as root, if not, quit with error message
if [[ $EUID -ne 0 ]]
then
    echo "[ERROR] Autostart VisiCam service script must be started as root, try sudo command"
    exit 1
fi

# ##################################################################
# MAIN
# ##################################################################

# Change working directory to local directory
cd "$(dirname $0)"

# Check for dummy file
if ! [ -e "visicam-service-template"]
then
    echo "[ERROR] Could not find file visicam-service-template"
    exit 1
fi

# Check if dummy file contains line which needs to be replaced
if ! ( ( cat "visicam-service-template" | grep '^VISICAMPATH="TEMPLATE-DUMMY"$' ) > "/dev/null" )
then
    echo "[ERROR] Could not find replace line in file visicam-service-template"
    exit 1
fi

# Get directory
DIRECTORY=$( pwd )

# Copy dummy file to target
cp "visicam-service-template" "/etc/init.d/visicam"

# Replace line in target file
sed -r -i "s/^VISICAMPATH=\"TEMPLATE-DUMMY\"$/VISICAMPATH=\"$DIRECTORY\"/g" "/etc/init.d/visicam"

# Add auto start entry for system boot, if it does not exist yet
if ! ( ( ls -l "/etc/rc2.d" | grep visicam ) > "/dev/null" )
then
    echo "[INFO] Creating auto start entry for visicam"
    update-rc.d visicam defaults
fi