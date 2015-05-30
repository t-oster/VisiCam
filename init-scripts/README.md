VisiCam init-scripts
====================

Installation Debian
------------

* copy the init-script to `/etc/init.d/visicam`
* mark the new init-script as executable `sudo chmod +x /etc/init.d/visicam`
* if you want to start VisiCam automatically on system-startup, execute `rc-update.d visicam defaults`

Installation Raspbian
------------

* Use the setup_service_raspbian.sh script in main folder as root
* It will ask for information and setup the automatic system start accordingly.

Usage
-----

```
sudo /etc/init.d/visicam {start|stop|status|restart|force-reload}
```
