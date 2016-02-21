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

package fr.gaellalire.vestige.jvm_enhancer.runtime.windows;

import java.util.Collection;
import java.util.IdentityHashMap;

import fr.gaellalire.vestige.core.function.Function;

/**
 * @author Gael Lalire
 */
public final class WindowsShutdownHook {

    private WindowsShutdownHook() {
    }

    private static IdentityHashMap<Thread, Boolean> hooks = new IdentityHashMap<Thread, Boolean>();

    public static final Function<Thread, Void, RuntimeException> ADD_SHUTDOWN_HOOK_FUNCTION = new Function<Thread, Void, RuntimeException>() {
        @Override
        public Void apply(final Thread thread) throws RuntimeException {
            addShutdownHook(thread);
            return null;
        }
    };

    public static final Function<Thread, Void, RuntimeException> REMOVE_SHUTDOWN_HOOK_FUNCTION = new Function<Thread, Void, RuntimeException>() {
        @Override
        public Void apply(final Thread thread) throws RuntimeException {
            removeShutdownHook(thread);
            return null;
        }
    };

    public static void init(final String libraryPath) {
        System.load(libraryPath);
        hooks = new IdentityHashMap<Thread, Boolean>();
        nativeRegister();
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                runHooks();
            }
        });
    };

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
    }

}
