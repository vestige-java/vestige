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

import fr.gaellalire.vestige.spi.system.VestigeSystem;
import fr.gaellalire.vestige.spi.system.VestigeSystemCache;

/**
 * @author Gael Lalire
 */
public class VestigeSystemHolder {

    private ThreadLocal<DefaultVestigeSystem> vestigeSystems = new InheritableThreadLocal<DefaultVestigeSystem>();

    private NullVestigeSystem nullVestigeSystem = new NullVestigeSystem(this);

    private DefaultVestigeSystem fallbackVestigeSystem;

    private VestigeSystemJarURLStreamHandler vestigeApplicationJarURLStreamHandler;

    private ThreadLocal<DefaultVestigeSystemCache> vestigeSystemCacheThreadLocal = new InheritableThreadLocal<DefaultVestigeSystemCache>();

    public DefaultVestigeSystem getVestigeSystem() {
        DefaultVestigeSystem vestigeSystem = vestigeSystems.get();
        if (vestigeSystem != null) {
            return vestigeSystem;
        }
        return fallbackVestigeSystem;
    }

    public VestigeSystem setVestigeSystem(final DefaultVestigeSystem vestigeSystem) {
        DefaultVestigeSystem pushedDefaultVestigeSystem = vestigeSystems.get();
        vestigeSystems.set(vestigeSystem);
        if (pushedDefaultVestigeSystem == null) {
            return nullVestigeSystem;
        }
        return pushedDefaultVestigeSystem;
    }

    public void setFallbackVestigeSystem(final DefaultVestigeSystem vestigeSystem) {
        fallbackVestigeSystem = vestigeSystem;
    }

    public void setVestigeApplicationJarURLStreamHandler(final VestigeSystemJarURLStreamHandler vestigeApplicationJarURLStreamHandler) {
        this.vestigeApplicationJarURLStreamHandler = vestigeApplicationJarURLStreamHandler;
    }

    public VestigeSystemJarURLStreamHandler getVestigeApplicationJarURLStreamHandler() {
        return vestigeApplicationJarURLStreamHandler;
    }

    public DefaultVestigeSystemCache getVestigeSystemCache() {
        return vestigeSystemCacheThreadLocal.get();
    }

    public VestigeSystemCache pushVestigeSystemCache() {
        DefaultVestigeSystemCache vestigeSystemCache = new DefaultVestigeSystemCache(this, vestigeSystemCacheThreadLocal.get());
        vestigeSystemCacheThreadLocal.set(vestigeSystemCache);
        return vestigeSystemCache;
    }

    public void clearCache(final DefaultVestigeSystemCache popedCache) {
        DefaultVestigeSystemCache vestigeSystemCache = vestigeSystemCacheThreadLocal.get();
        while (vestigeSystemCache != null && vestigeSystemCache != popedCache) {
            vestigeSystemCache = vestigeSystemCache.getParent();
        }
        if (vestigeSystemCache == null) {
            // can not clear cache
            return;
        }
        vestigeSystemCache = vestigeSystemCacheThreadLocal.get();
        boolean last = false;
        while (!last) {
            if (vestigeSystemCache == popedCache) {
                last = true;
            }
            vestigeSystemCache.doClearCache();

            vestigeSystemCache = vestigeSystemCache.getParent();
        }
        vestigeSystemCacheThreadLocal.set(vestigeSystemCache);

        vestigeApplicationJarURLStreamHandler.clearCache();

    }

}
