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

package com.googlecode.vestige.application;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.WeakHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Gael Lalire
 */
public class ThreadGroupDestroyer extends Thread {

    private static final Logger LOGGER = LoggerFactory.getLogger(ThreadGroupDestroyer.class);

    public ThreadGroupDestroyer() {
        super("ThreadGroupDestroyer");
    }

    private Map<Thread, ThreadGroup> threadGroupToDestroyByThread = new WeakHashMap<Thread, ThreadGroup>();

    private List<ThreadGroup> threadGroups = new ArrayList<ThreadGroup>();

    public void destroy(final Thread thread, final ThreadGroup threadGroup) {
        synchronized (threadGroupToDestroyByThread) {
            threadGroupToDestroyByThread.put(thread, threadGroup);
            threadGroupToDestroyByThread.notify();
        }
    }

    @Override
    public void run() {
        try {
            while (true) {
                synchronized (threadGroupToDestroyByThread) {
                    Iterator<ThreadGroup> iterator = threadGroups.iterator();
                    while (iterator.hasNext()) {
                        ThreadGroup threadGroup = iterator.next();
                        try {
                            threadGroup.destroy();
                            iterator.remove();
                            if (LOGGER.isInfoEnabled()) {
                                LOGGER.info("ThreadGroup {} is GC", threadGroup.getName());
                            }
                        } catch (IllegalThreadStateException e) {
                            LOGGER.trace("ThreadGroup still not empty", e);
                        }
                    }
                    for (Entry<Thread, ThreadGroup> entry : threadGroupToDestroyByThread.entrySet()) {
                        Thread key = entry.getKey();
                        if (key.isAlive()) {
                            // this thread is about to die
                            key.join();
                        }
                        ThreadGroup value = entry.getValue();
                        try {
                            value.destroy();
                        } catch (IllegalThreadStateException e) {
                            if (LOGGER.isErrorEnabled()) {
                                LOGGER.error("Thread group {} is not empty after its main thread die, the application has thread leak", value.getName());
                            }
                            int activeCount = value.activeCount();
                            Thread[] list = new Thread[activeCount];
                            int enumerate = value.enumerate(list);
                            for (int i = 0; i < enumerate; i++) {
                                if (LOGGER.isErrorEnabled()) {
                                    LOGGER.error("Alive thread", new ThreadStackTraceException(list[i]));
                                }
                            }
                            threadGroups.add(value);
                        }
                    }
                    threadGroupToDestroyByThread.clear();
                    threadGroupToDestroyByThread.wait();
                }
            }
        } catch (InterruptedException e) {
            LOGGER.trace("ThreadGroupDestroyer stop", e);
        }
    }

}
