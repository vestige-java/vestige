#!/usr/bin/python
import gtk
import os
import signal
import gobject

def killVestige(icon):
    os.kill(vestigePid, signal.SIGTERM)

class StatusIcon:
    def __init__(self):
        self.loadStatusIcon = False
        if not os.environ.get("XDG_CURRENT_DESKTOP").lower().startswith("gnome") or not "deprecated" in os.environ.get('GNOME_DESKTOP_SESSION_ID'):
            try:
                import appindicator
                self.ind = appindicator.Indicator("vestige", "vestige", appindicator.CATEGORY_APPLICATION_STATUS)
                self.ind.set_status(appindicator.STATUS_ACTIVE)
                self.ind.set_menu(gtk.Menu());
            except ImportError:
                # appindicator not found
                self.loadStatusIcon = True
        else:
            # gnome 3 status icon has a scrolling bug which occurs with appindicator fallback implementation
            self.loadStatusIcon = True
                  
        if self.loadStatusIcon:
            self.statusicon = gtk.StatusIcon()
            self.statusicon.set_from_icon_name("vestige")
            self.statusicon.connect("popup-menu", self.right_click_event)
            self.statusicon.connect("activate", self.left_click_event)
            self.statusicon.set_tooltip("Vestige")
        
        self.menu = gtk.Menu()
        self.quit = gtk.MenuItem("Quit")
        self.quit.connect("activate", killVestige)
        self.menu.append(self.quit)
        self.menu.show_all()
        
        if not self.loadStatusIcon:
            self.ind.set_menu(self.menu)

    def main(self):
            # tell .desktop
            gtk.gdk.notify_startup_complete()
            # main loop
            gtk.main()

    def left_click_event(self, icon):
        self.menu.popup(None, None, None, 0, gtk.get_current_event_time(), icon)

    def right_click_event(self, icon, button, activate_time):
        self.menu.popup(None, None, None, button, activate_time, icon)
    
def child_cb(pid, status):
    gtk.main_quit()

vestigePid = os.fork()
if vestigePid == 0:
    os.execlp("/usr/share/vestige/vestige", "/usr/share/vestige/vestige")
    os._exit(1)

gobject.child_watch_add(vestigePid, child_cb)

statusIcon = StatusIcon()
statusIcon.main()

