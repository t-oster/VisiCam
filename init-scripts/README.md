VisiCam init-scripts
====================

Currently only Debian is supported.

Installation
------------

* copy the init-script to `/etc/init.d/visicam`
* mark the new init-script as executable `sudo chmod +x /etc/init.d/visicam`
* if you want to start VisiCam automatically on system-startup, execute `rc-update.d visicam defaults`

Usage
-----

```
sudo /etc/init.d/visicam {start|stop|status|restart|force-reload}
```
