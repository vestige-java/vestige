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

package fr.gaellalire.vestige.application.manager;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

/**
 * @author Gael Lalire
 * @param <E>
 */
public class SelfThreadVestigeSecureExecution<E> implements VestigeSecureExecution<E> {

    private volatile Thread thread;

    private FutureTask<E> futureTask;

    public SelfThreadVestigeSecureExecution(final FutureTask<E> futureTask) {
        this.futureTask = futureTask;
    }

    public void start() {
        thread = Thread.currentThread();
        try {
            futureTask.run();
        } finally {
            thread = null;
        }
    }

    public void interrupt() {
        Thread thread = this.thread;
        if (thread != null) {
            thread.interrupt();
        }
    }

    public E get() throws Exception {
        do {
            try {
                return futureTask.get();
            } catch (InterruptedException e) {
                interrupt();
            } catch (ExecutionException e) {
                Throwable cause = e.getCause();
                if (cause instanceof Exception) {
                    throw (Exception) cause;
                } else {
                    throw e;
                }
            }
        } while (true);
    }

    public void join() throws InterruptedException {
        Thread thread = this.thread;
        if (thread != null) {
            thread.join();
        }
    }

    public ThreadGroup getThreadGroup() {
        Thread thread = this.thread;
        if (thread != null) {
            return thread.getThreadGroup();
        }
        return null;
    }

}
