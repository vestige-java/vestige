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

import java.util.concurrent.Callable;

import fr.gaellalire.vestige.core.VestigeClassLoader;
import fr.gaellalire.vestige.platform.AttachedVestigeClassLoader;
import fr.gaellalire.vestige.platform.system.PublicVestigeSystem;

/**
 * @author Gael Lalire
 */
public class RuntimeApplicationContext {

    private VestigeClassLoader<AttachedVestigeClassLoader> classLoader;

    private Callable<?> applicationCallable;

    private PublicVestigeSystem vestigeSystem;

    private boolean runAllowed;

    public RuntimeApplicationContext(final VestigeClassLoader<AttachedVestigeClassLoader> classLoader, final Callable<?> applicationCallable, final PublicVestigeSystem vestigeSystem, final boolean runAllowed) {
        this.classLoader = classLoader;
        this.applicationCallable = applicationCallable;
        this.vestigeSystem = vestigeSystem;
        this.runAllowed = runAllowed;
    }

    public VestigeClassLoader<AttachedVestigeClassLoader> getClassLoader() {
        return classLoader;
    }

    public Callable<?> getApplicationCallable() {
        return applicationCallable;
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
