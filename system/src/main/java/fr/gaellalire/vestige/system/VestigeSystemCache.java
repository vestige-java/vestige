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

package fr.gaellalire.vestige.system;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Gael Lalire
 */
public class VestigeSystemCache implements PublicVestigeSystemCache {

    private VestigeSystemCache parent;

    private List<WeakReference<CachedJarFile>> weakReferences;

    private VestigeSystemJarURLStreamHandler vestigeApplicationJarURLStreamHandler;

    public VestigeSystemCache(final VestigeSystemJarURLStreamHandler vestigeApplicationJarURLStreamHandler, final VestigeSystemCache parent) {
        this.vestigeApplicationJarURLStreamHandler = vestigeApplicationJarURLStreamHandler;
        this.parent = parent;
        this.weakReferences = new ArrayList<WeakReference<CachedJarFile>>();
    }

    @Override
    public void clearCache() {
        vestigeApplicationJarURLStreamHandler.clearCache(this);
    }

    public VestigeSystemCache getParent() {
        return parent;
    }

    public List<WeakReference<CachedJarFile>> getWeakReferences() {
        return weakReferences;
    }

}
