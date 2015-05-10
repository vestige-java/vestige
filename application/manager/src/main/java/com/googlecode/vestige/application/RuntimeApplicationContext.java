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

import com.googlecode.vestige.core.VestigeClassLoader;
import com.googlecode.vestige.platform.AttachedVestigeClassLoader;
import com.googlecode.vestige.platform.system.PublicVestigeSystem;

/**
 * @author Gael Lalire
 */
public class RuntimeApplicationContext {

    private VestigeClassLoader<AttachedVestigeClassLoader> classLoader;

    private Runnable runnable;

    private PublicVestigeSystem vestigeSystem;

    private boolean runAllowed;

    public RuntimeApplicationContext(final VestigeClassLoader<AttachedVestigeClassLoader> classLoader, final Runnable runnable, final PublicVestigeSystem vestigeSystem, final boolean runAllowed) {
        this.classLoader = classLoader;
        this.runnable = runnable;
        this.vestigeSystem = vestigeSystem;
        this.runAllowed = runAllowed;
    }

    public VestigeClassLoader<AttachedVestigeClassLoader> getClassLoader() {
        return classLoader;
    }

    public Runnable getRunnable() {
        return runnable;
    }

    public PublicVestigeSystem getVestigeSystem() {
        return vestigeSystem;
    }

    public void setRunAllowed(final boolean runAllowed) {
        this.runAllowed = runAllowed;
    }

    public boolean isRunAllowed() {
        return runAllowed;
    }

}
