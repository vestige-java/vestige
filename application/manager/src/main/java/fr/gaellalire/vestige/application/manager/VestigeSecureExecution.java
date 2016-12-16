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
import java.util.concurrent.Future;

/**
 * @author Gael Lalire
 * @param <E>
 */
public class VestigeSecureExecution<E> {

    private Thread thread;

    private Future<E> future;

    public VestigeSecureExecution(final Thread thread, final Future<E> future) {
        this.thread = thread;
        this.future = future;
    }

    public void start() {
        thread.start();
    }

    public void interrupt() {
        thread.interrupt();
    }

    public E get() throws Exception {
        try {
            return future.get();
        } catch (InterruptedException e) {
            thread.interrupt();
            throw e;
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Exception) {
                throw (Exception) cause;
            } else {
                throw e;
            }
        }
    }

    public void join() throws InterruptedException {
        thread.join();
    }

    public ThreadGroup getThreadGroup() {
        return thread.getThreadGroup();
    }

}
