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
if ! [ -e "init-scripts/raspbian_template" ]
then
    echo "[ERROR] Could not find file init-scripts/raspbian_template"
    exit 1
fi

# Check for local jar path
if ! [ -e "dist/VisiCam.jar" ]
then
    echo "[ERROR] Could not find file dist/VisiCam.jar"
    exit 1
fi

# Check if dummy file contains lines which need to be replaced
if ! ( ( cat "init-scripts/raspbian_template" | grep '^VISICAM_JAR=TEMPLATE-LOCAL-JAR-PATH$' ) > "/dev/null" )
then
    echo "[ERROR] Could not find replace line VISICAM_JAR in file init-scripts/raspbian_template"
    exit 1
fi

if ! ( ( cat "init-scripts/raspbian_template" | grep '^VISICAM_ROOT=TEMPLATE-ABSOLUTE-ROOT-PATH$' ) > "/dev/null" )
then
    echo "[ERROR] Could not find replace line VISICAM_ROOT in file init-scripts/raspbian_template"
    exit 1
fi

if ! ( ( cat "init-scripts/raspbian_template" | grep '^PORT=TEMPLATE-PORT$' ) > "/dev/null" )
then
    echo "[ERROR] Could not find replace line PORT in file init-scripts/raspbian_template"
    exit 1
fi

if ! ( ( cat "init-scripts/raspbian_template" | grep '^DAEMONUSER=TEMPLATE-USER$' ) > "/dev/null" )
then
    echo "[ERROR] Could not find replace line DAEMONUSER in file init-scripts/raspbian_template"
    exit 1
fi

if ! ( ( cat "init-scripts/raspbian_template" | grep '^DAEMONGROUP=TEMPLATE-GROUP$' ) > "/dev/null" )
then
    echo "[ERROR] Could not find replace line DAEMONGROUP in file init-scripts/raspbian_template"
    exit 1
fi

# Get directory
DIRECTORY=$( pwd )

# Ask for port
echo ""
echo "[INFO] Please enter the web port at which your VisiCam installation should start."
echo "[INFO] (Default: 8080)"
read -p "Port: " PORT
echo ""

# Ask for user
echo "[INFO] Please enter the name of the user, who is used to automatically start the service."
echo "[INFO] Note: User must have write access to /var/run/ and /var/log/ !"
echo "[INFO] (Default: root)"
read -p "User: " USER
echo ""

# Copy dummy file to target
cp "init-scripts/raspbian_template" "/etc/init.d/visicam"

# Check if copy was successful
if [ $? -ne 0 ]
then
    echo "[ERROR] Could not copy init-scripts/raspbian_template file to: /etc/init.d/visicam"
    exit 1
fi

# Change permission settings for script
chmod +x "/etc/init.d/visicam"

# Check if chmod was successful
if [ $? -ne 0 ]
then
    echo "[ERROR] Could not add execute permission to file /etc/init.d/visicam"
    exit 1
fi

# Replace lines in target file
# Note: Need to change delimiter for sed from / to : since slashes might be part of directory path
sed -r -i "s:^VISICAM_JAR=TEMPLATE-LOCAL-JAR-PATH$:VISICAM_JAR=dist/VisiCam.jar:g" "/etc/init.d/visicam"
if [ $? -ne 0 ]
then
    echo "[ERROR] Could not replace line VISICAM_JAR in file /etc/init.d/visicam"
    exit 1
fi

sed -r -i "s:^VISICAM_ROOT=TEMPLATE-ABSOLUTE-ROOT-PATH$:VISICAM_ROOT=$DIRECTORY/:g" "/etc/init.d/visicam"
if [ $? -ne 0 ]
then
    echo "[ERROR] Could not replace line VISICAM_ROOT in file /etc/init.d/visicam"
    exit 1
fi

sed -r -i "s:^PORT=TEMPLATE-PORT$:PORT=$PORT:g" "/etc/init.d/visicam"
if [ $? -ne 0 ]
then
    echo "[ERROR] Could not replace line PORT in file /etc/init.d/visicam"
    exit 1
fi

sed -r -i "s:^DAEMONUSER=TEMPLATE-USER$:DAEMONUSER=$USER:g" "/etc/init.d/visicam"
if [ $? -ne 0 ]
then
    echo "[ERROR] Could not replace line DAEMONUSER in file /etc/init.d/visicam"
    exit 1
fi

sed -r -i "s:^DAEMONGROUP=TEMPLATE-GROUP$:DAEMONGROUP=$USER:g" "/etc/init.d/visicam"
if [ $? -ne 0 ]
then
    echo "[ERROR] Could not replace line DAEMONGROUP in file /etc/init.d/visicam"
    exit 1
fi

# Add auto start entry for system boot, if it does not exist yet
if ! ( ( ls -l "/etc/rc2.d" | grep visicam ) > "/dev/null" )
then
    echo "[INFO] Creating auto start entry for visicam"
    update-rc.d visicam defaults
fi

# Success message
echo "[INFO] Service visicam successfully configured for automatic system start"