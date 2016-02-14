/*
 * This file is part of Vestige.
 *
 * Vestige is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Vestige is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Vestige.  If not, see <http://www.gnu.org/licenses/>.
 */

package fr.gaellalire.vestige.jvm_enhancer.windows;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.IdentityHashMap;

/**
 * @author Gael Lalire
 */
public final class WindowsShutdownHook {

    private WindowsShutdownHook() {
    }

    private static IdentityHashMap<Thread, Boolean> hooks;

    private static File shutdownHookDLL;

    static {
        try {
            shutdownHookDLL = File.createTempFile("shHo", ".dll");
            String arch = "w32";
            if (!System.getProperty("os.arch").equals("x86")) {
                arch = System.getProperty("os.arch");
                if (arch.equals("x86_64")) {
                    arch = "amd64";
                }
            }
            InputStream source = WindowsShutdownHook.class.getResourceAsStream("WindowsShutdownHook_" + arch + ".dll");
            if (source == null) {
                shutdownHookDLL.delete();
                throw new RuntimeException("Arch " + arch + " unknown");
            }
            try {
                FileOutputStream destination = new FileOutputStream(shutdownHookDLL.getAbsolutePath());
                try {
                    byte[] buffer = new byte[1024];
                    int read = 0;
                    while (read >= 0) {
                        destination.write(buffer, 0, read);
                        read = source.read(buffer);
                    }
                } finally {
                    destination.close();
                }
            } finally {
                source.close();
            }
        } catch (IOException e) {
            shutdownHookDLL.delete();
            throw new RuntimeException("Cannot load WindowsShutdownHook.dll", e);
        }
        System.load(shutdownHookDLL.getAbsolutePath());
        hooks = new IdentityHashMap<Thread, Boolean>();
        nativeRegister();
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                runHooks();
            }
        });
    }

    private static native void nativeRegister();

    public static synchronized void addShutdownHook(final Thread hook) {
        if (hooks == null) {
            throw new IllegalStateException("Shutdown in progress");
        }

        if (hook.isAlive()) {
            throw new IllegalArgumentException("Hook already running");
        }

        if (hooks.containsKey(hook)) {
            throw new IllegalArgumentException("Hook previously registered");
        }

        hooks.put(hook, Boolean.TRUE);
    }

    public static synchronized boolean removeShutdownHook(final Thread hook) {
        if (hooks == null) {
            throw new IllegalStateException("Shutdown in progress");
        }

        if (hook == null) {
            throw new NullPointerException();
        }

        return hooks.remove(hook) != null;
    }

    private static Collection<Thread> threads;

    private static void runHooks() {
        synchronized (WindowsShutdownHook.class) {
            if (hooks != null) {
                threads = hooks.keySet();
                hooks = null;
                for (Thread hook : threads) {
                    hook.start();
                }
            }
        }
        for (Thread hook : threads) {
            try {
                hook.join();
            } catch (InterruptedException x) {
                // nothing
            }
        }
        shutdownHookDLL.delete();
    }

}
