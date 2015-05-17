#!/usr/bin/python
import gtk
import os
import signal
import gobject

def killVestige(icon):
    os.kill(vestigePid, signal.SIGTERM)    

class StatusIcon:
    def __init__(self):
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

gtk.gdk.notify_startup_complete()

StatusIcon()
gtk.main()

