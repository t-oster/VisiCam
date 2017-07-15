VisiCam init-scripts
====================

Installation using systemd
---

* copy the unit file `visicam.service` to `/etc/systemd/system/visicam.service`
* enable the VisiCam unit to run at system start: `systemctl enable visicam.service`
* start VisiCam: `systemctl start visicam.service`

Installation using init.d
---

### Debian

* copy the init-script to `/etc/init.d/visicam`
* mark the new init-script as executable `sudo chmod +x /etc/init.d/visicam`
* if you want to start VisiCam automatically on system-startup, execute `rc-update.d visicam defaults`

### Raspbian

* Use the setup_service_raspbian.sh script in main folder as root
* It will ask for information and setup the automatic system start accordingly.

### Usage

```
sudo /etc/init.d/visicam {start|stop|status|restart|force-reload}
```
