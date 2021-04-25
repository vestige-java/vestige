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

package fr.gaellalire.vestige.system.interceptor;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import fr.gaellalire.vestige.core.StackedHandler;
import fr.gaellalire.vestige.system.VestigeSystemHolder;

/**
 * @author Gael Lalire
 */
public class TCPTransportConnectionThreadPool implements ExecutorService, StackedHandler<ExecutorService> {

    private static final int MAX_CONNECTION_THREADS = Integer.getInteger("sun.rmi.transport.tcp.maxConnectionThreads", Integer.MAX_VALUE);

    private static final long THREAD_KEEP_ALIVE_TIME = Long.getLong("sun.rmi.transport.tcp.threadKeepAliveTime", 60000);

    public static ExecutorService createConnectionThreadPool() {

        return new ThreadPoolExecutor(0, MAX_CONNECTION_THREADS, THREAD_KEEP_ALIVE_TIME, TimeUnit.MILLISECONDS, new SynchronousQueue<Runnable>(), new ThreadFactory() {
            public Thread newThread(final Runnable runnable) {
                Thread t = new Thread(runnable, "RMI TCP Connection(idle)");
                t.setContextClassLoader(ClassLoader.getSystemClassLoader());
                t.setDaemon(true);
                return t;
            }
        });
    }

    private VestigeSystemHolder vestigeSystemHolder;

    private ExecutorService nextHandler;

    public TCPTransportConnectionThreadPool(final VestigeSystemHolder vestigeSystemHolder) {
        this.vestigeSystemHolder = vestigeSystemHolder;
    }

    @Override
    public void execute(final Runnable command) {
        ExecutorService tcpTransportConnectionThreadPool = vestigeSystemHolder.getVestigeSystemCache().getTcpTransportConnectionThreadPoolReference().get();
        tcpTransportConnectionThreadPool.execute(command);
    }

    @Override
    public ExecutorService getNextHandler() {
        return nextHandler;
    }

    @Override
    public void setNextHandler(final ExecutorService nextHandler) {
        this.nextHandler = nextHandler;
    }

    @Override
    public void shutdown() {
    }

    @Override
    public List<Runnable> shutdownNow() {
        return null;
    }

    @Override
    public boolean isShutdown() {
        return false;
    }

    @Override
    public boolean isTerminated() {
        return false;
    }

    @Override
    public boolean awaitTermination(final long timeout, final TimeUnit unit) throws InterruptedException {
        return false;
    }

    @Override
    public <T> Future<T> submit(final Callable<T> task) {
        return null;
    }

    @Override
    public <T> Future<T> submit(final Runnable task, final T result) {
        return null;
    }

    @Override
    public Future<?> submit(final Runnable task) {
        return null;
    }

    @Override
    public <T> List<Future<T>> invokeAll(final Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return null;
    }

    @Override
    public <T> List<Future<T>> invokeAll(final Collection<? extends Callable<T>> tasks, final long timeout, final TimeUnit unit) throws InterruptedException {
        return null;
    }

    @Override
    public <T> T invokeAny(final Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        return null;
    }

    @Override
    public <T> T invokeAny(final Collection<? extends Callable<T>> tasks, final long timeout, final TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        return null;
    }

}
