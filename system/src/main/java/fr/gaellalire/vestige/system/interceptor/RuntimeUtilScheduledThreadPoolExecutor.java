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

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import fr.gaellalire.vestige.core.StackedHandler;
import fr.gaellalire.vestige.spi.system.VestigeSystem;
import fr.gaellalire.vestige.system.VestigeSystemHolder;

/**
 * @author Gael Lalire
 */
public class RuntimeUtilScheduledThreadPoolExecutor extends ScheduledThreadPoolExecutor implements StackedHandler<ScheduledThreadPoolExecutor> {

    private static final int SCHEDULER_THREADS = Integer.getInteger("sun.rmi.runtime.schedulerThreads", 1);

    public static void setSystemThreadGroup(final ThreadGroup systemThreadGroup) {
        RuntimeUtilScheduledThreadPoolExecutor.systemThreadGroup = systemThreadGroup;
    }

    private ScheduledThreadPoolExecutor nextHandler;

    private static ThreadGroup systemThreadGroup;

    private VestigeSystemHolder vestigeSystemHolder;

    public RuntimeUtilScheduledThreadPoolExecutor(final VestigeSystemHolder vestigeSystemHolder) {
        super(0, new ThreadFactory() {

            @Override
            public Thread newThread(final Runnable r) {
                return new Thread(r);
            }
        });
        this.vestigeSystemHolder = vestigeSystemHolder;
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(final Runnable command, final long initialDelay, final long delay, final TimeUnit unit) {
        VestigeSystem handlerVestigeSystem = vestigeSystemHolder.getHandlerVestigeSystem();
        VestigeSystem pushedVestigeSystem = handlerVestigeSystem.setCurrentSystem();
        try {
            return vestigeSystemHolder.getVestigeSystemCache().getScheduledThreadPoolExecutorReference().get().scheduleWithFixedDelay(command, initialDelay, delay, unit);
        } finally {
            pushedVestigeSystem.setCurrentSystem();
        }
    }

    @Override
    public ScheduledFuture<?> schedule(final Runnable command, final long delay, final TimeUnit unit) {
        VestigeSystem handlerVestigeSystem = vestigeSystemHolder.getHandlerVestigeSystem();
        VestigeSystem pushedVestigeSystem = handlerVestigeSystem.setCurrentSystem();
        try {
            return vestigeSystemHolder.getVestigeSystemCache().getScheduledThreadPoolExecutorReference().get().schedule(command, delay, unit);
        } finally {
            pushedVestigeSystem.setCurrentSystem();
        }
    }

    @Override
    public ScheduledThreadPoolExecutor getNextHandler() {
        return nextHandler;
    }

    @Override
    public void setNextHandler(final ScheduledThreadPoolExecutor nextHandler) {
        this.nextHandler = nextHandler;
    }

    public static ScheduledThreadPoolExecutor createScheduledThreadPoolExecutor() {
        return new ScheduledThreadPoolExecutor(SCHEDULER_THREADS, new ThreadFactory() {
            private final AtomicInteger count = new AtomicInteger(0);

            public Thread newThread(final Runnable runnable) {
                Thread t = new Thread(systemThreadGroup, runnable, "RMI Scheduler(" + count.getAndIncrement() + ")");
                t.setContextClassLoader(ClassLoader.getSystemClassLoader());
                t.setDaemon(true);
                return t;
            }
        });
    }

}
