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

package com.googlecode.vestige.platform.system;

import java.security.Permission;
import java.security.Policy;
import java.security.ProtectionDomain;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Gael Lalire
 */
public class PrivateWhiteListVestigePolicy extends Policy {

    private Set<ClassLoader> safeClassLoaders = new HashSet<ClassLoader>();

    private ProtectionDomain selfProtectionDomain;

    public PrivateWhiteListVestigePolicy() {
        selfProtectionDomain = PrivateWhiteListVestigePolicy.class.getProtectionDomain();
    }

    public void clearSafeClassLoader() {
        safeClassLoaders.clear();
    }

    public void addSafeClassLoader(final ClassLoader safeClassLoader) {
        ClassLoader classLoader = safeClassLoader;
        while (classLoader != null) {
            safeClassLoaders.add(classLoader);
            classLoader = classLoader.getParent();
        }
    }

    @Override
    public boolean implies(final ProtectionDomain domain, final Permission permission) {
        if (selfProtectionDomain == domain) {
            return true;
        }
        ClassLoader classLoader = domain.getClassLoader();
        for (ClassLoader safeClassLoader : safeClassLoaders) {
            if (safeClassLoader == classLoader) {
                // safeClassLoader are not restricted
                return true;
            }
        }
        return false;
    }

}
