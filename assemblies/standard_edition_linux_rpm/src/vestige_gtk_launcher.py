#!/usr/bin/python
import dbus
import dbus.bus
import dbus.service
import dbus.mainloop.glib
import gobject
import gtk
import gtk.gdk
import os
import signal
import gobject
import subprocess
import socket
import time

class Vestige(dbus.service.Object):
    def __init__(self, bus, path, name):
        dbus.service.Object.__init__(self, bus, path, name)
        self.procState = 0
        self.openCount = 0
        self.running = False
        self.url = None
        self.loadStatusIcon = False
        self.consoleWinShown = False
        
        if not os.environ.get("DESKTOP_SESSION", "").lower().startswith("gnome") or not "deprecated" in os.environ.get("GNOME_DESKTOP_SESSION_ID", ""):
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
        
            
        self.consoleWin = gtk.Window()
        self.consoleWin.set_title("Vestige: command line output")
        self.consoleWin.set_default_size(700,500)
        self.consoleWin.set_position(gtk.WIN_POS_CENTER_ALWAYS)
        self.consoleWin.connect('delete-event', lambda w, e : self.hideWin())
        self.console = gtk.TextView()
        self.console.set_editable(False)
        scroller = gtk.ScrolledWindow()
        scroller.add(self.console)
        self.consoleWin.add(scroller)
    
        serversocket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        serversocket.bind(('127.0.0.1', 0))
        serversocket.listen(1)
        gobject.io_add_watch(serversocket, gobject.IO_IN, self.listener)        

        procenv = os.environ.copy()
        
        procenv["VESTIGE_LISTENER_PORT"] = str(serversocket.getsockname()[1]);
        try:
            self.proc = subprocess.Popen("/usr/share/vestige/vestige", env = procenv, shell = False, stdout = subprocess.PIPE, stderr = subprocess.PIPE)
            gobject.io_add_watch(self.proc.stdout, gobject.IO_IN, self.write_to_buffer)
            gobject.io_add_watch(self.proc.stderr, gobject.IO_IN, self.write_to_buffer)
            gobject.child_watch_add(self.proc.pid, lambda pid, condition: self.processQuit())
        except:
            buf = self.console.get_buffer()
            buf.insert_at_cursor("Error: vestige script cannot be launched")
            self.processQuit();

        self.menu = gtk.Menu()
        
        self.adminItem = gtk.MenuItem("Open web administration")
        self.adminItem.connect("activate", lambda e : self.showAdmin())
        self.menu.append(self.adminItem)
        self.adminItem.set_sensitive(False)

        self.folderItem = gtk.MenuItem("Open base folder")
        self.folderItem.connect("activate", lambda e : self.openFolder())
        self.menu.append(self.folderItem)
        self.folderItem.set_sensitive(False)

        consoleItem = gtk.MenuItem("Show command line output")
        consoleItem.connect("activate", lambda e : self.showWin())
        self.menu.append(consoleItem)
        
        self.menu.append(gtk.SeparatorMenuItem())

        quitItem = gtk.MenuItem("Quit")
        quitItem.connect("activate", lambda e : self.terminateProc())
        self.menu.append(quitItem)

        self.menu.show_all()
        
        if not self.loadStatusIcon:
            self.ind.set_menu(self.menu)

        
    def processQuit(self):
        if self.consoleWinShown or self.procState < 2:
            # user show console or starting failed
            self.procState = 5
            if self.loadStatusIcon:
                self.statusicon.set_visible(False)
            else:
                import appindicator
                self.ind.set_status(appindicator.STATUS_PASSIVE)
            self.showWin();
        else:
            gtk.main_quit()
            
    def terminateProc(self):
        try:
            if self.proc is not None:
                self.proc.terminate();
        except:
            pass
            
    def openFolder(self):
        env = os.environ.copy()
        self.openCount += 1
        env["DESKTOP_STARTUP_ID"] = "vestige-xdg_open-%d_TIME%d" % (self.openCount, time.time())
        subprocess.Popen(["xdg-open", self.baseFolder], env=env)

    def showAdmin(self):
        env = os.environ.copy()
        self.openCount += 1
        env["DESKTOP_STARTUP_ID"] = "vestige-xdg_open-%d_TIME%d" % (self.openCount, time.time())
        subprocess.Popen(["xdg-open", self.url], env=env)
            
    def showWin(self):
        self.consoleWinShown = True
        self.consoleWin.show_all()
        self.consoleWin.present_with_time(int(time.time()))
        self.consoleWin.window.focus()

    def hideWin(self):
        if self.procState == 5:
            gtk.main_quit()
        else:
            self.consoleWinShown = False
            self.consoleWin.hide();
        return True
            
    def listener(self, sock, arg):
        conn, addr = sock.accept()
        gobject.io_add_watch(conn, gobject.IO_IN, self.handler)
        return True    
    
    def handler(self, conn, args):
        lines = conn.recv(1024)
        if not len(lines):
            return False

        for line in lines.splitlines():
            if line.startswith("Web "):
                self.url = line[len("Web "):]
                self.adminItem.set_sensitive(True)
            elif line.startswith("Base "):
                self.baseFolder = line[len("Base "):]
                self.folderItem.set_sensitive(True)
            elif line == "Starting":
                self.procState = 1
            elif line == "Started":
                self.procState = 2
            elif line == "Stopping":
                self.procState = 3
            elif line == "Stopped":
                self.procState = 4
                
        return True
        
    def write_to_buffer(self, fd, condition):
        if condition == gobject.IO_IN:
            char = fd.read(1)
            buf = self.console.get_buffer()
            buf.place_cursor(buf.get_end_iter());
            buf.insert_at_cursor(char)
            return True
        else:
            return True

    def left_click_event(self, icon):
        self.menu.popup(None, None, None, 0, gtk.get_current_event_time(), icon)

    def right_click_event(self, icon, button, activate_time):
        self.menu.popup(None, None, None, button, activate_time, icon)
        
    @dbus.service.method("fr.gaellalire.vestige", in_signature='', out_signature='')
    def handleFiles(self):
        # TODO add file name array
        pass
     
app = None
def signalHandler(signal, frame):
    if app is not None:
        app.terminateProc();

signal.signal(signal.SIGTERM, signalHandler)
try:     
    dbus.mainloop.glib.DBusGMainLoop(set_as_default=True)
    bus = dbus.SessionBus()
    local = False
    request = bus.request_name("fr.gaellalire.vestige", dbus.bus.NAME_FLAG_DO_NOT_QUEUE)
    if request != dbus.bus.REQUEST_NAME_REPLY_EXISTS:
        app = Vestige(bus, '/', "fr.gaellalire.vestige")
        local = True
    else:
        object = bus.get_object("fr.gaellalire.vestige", "/")
        app = dbus.Interface(object, "fr.gaellalire.vestige")
    
    app.handleFiles()
    gtk.gdk.notify_startup_complete()
    
    if local:
        gtk.main()
except KeyboardInterrupt:
    if app is not None:
        app.terminateProc();
