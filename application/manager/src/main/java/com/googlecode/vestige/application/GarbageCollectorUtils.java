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

import java.lang.ref.SoftReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Gael Lalire
 */
public final class GarbageCollectorUtils {

    private GarbageCollectorUtils() {
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(GarbageCollectorUtils.class);

    /**
     * This method allocate 75% of free memory twice. The first allocation is
     * kept in a {@link SoftReference}, so the second should success because the
     * first is GC. This method should only be used in development to track
     * memory leaks. If there is multiple thread you can produce an
     * {@link OutOfMemoryError} in another thread, so you should suspend all
     * other thread before calling this method.
     * @return true if one soft reference has been GC
     */
    public static boolean forceSoftReferenceGC() {
        Runtime runtime = Runtime.getRuntime();
        runtime.gc();
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory() + maxMemory - totalMemory;

        boolean done = false;
        // 1 long = 8 bytes, (freeMemory / 8) * (75 / 100) ~= freeMemory / 11
        long alloc = freeMemory / 11;
        try {
            SoftReference<Object> sr;
            if (alloc >= Integer.MAX_VALUE) {
                int m = (int) (alloc / Integer.MAX_VALUE);
                long[][] tt = new long[m][Integer.MAX_VALUE];
                sr = new SoftReference<Object>(tt);
                tt = null;
                tt = new long[m][Integer.MAX_VALUE];
                tt = null;
            } else {
                long[] t = new long[(int) alloc];
                sr = new SoftReference<Object>(t);
                t = null;
                t = new long[(int) alloc];
                t = null;
            }
            if (sr.get() == null) {
                done = true;
            }
            sr.clear();
            sr = null;
            runtime.gc();
        } catch (OutOfMemoryError e) {
            runtime.gc();
            LOGGER.trace("Error", e);
        }
        return done;
    }

}
