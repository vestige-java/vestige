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

package fr.gaellalire.vestige.edition.maven_main_launcher;

import java.io.Serializable;
import java.util.List;

/**
 * @author Gael Lalire
 */
public class MavenResolverCache implements Serializable {

    private static final long serialVersionUID = 561193783052759466L;

    private List<VerifiedClassLoaderConfiguration> launchCaches;

    private String className;

    private VerifiedClassLoaderConfiguration classLoaderConfiguration;

    private long lastModified;

    public MavenResolverCache(final List<VerifiedClassLoaderConfiguration> classLoaderConfigurations, final String className,
            final VerifiedClassLoaderConfiguration classLoaderConfiguration, final long lastModified) {
        this.launchCaches = classLoaderConfigurations;
        this.className = className;
        this.classLoaderConfiguration = classLoaderConfiguration;
        this.lastModified = lastModified;
    }

    public List<VerifiedClassLoaderConfiguration> getClassLoaderConfigurations() {
        return launchCaches;
    }

    public long getLastModified() {
        return lastModified;
    }

    public String getClassName() {
        return className;
    }

    public VerifiedClassLoaderConfiguration getClassLoaderConfiguration() {
        return classLoaderConfiguration;
    }

    public boolean verify() {
        boolean result = true;
        if (!classLoaderConfiguration.verify()) {
            result = false;
        }
        for (VerifiedClassLoaderConfiguration launchCache : launchCaches) {
            if (launchCache.verify()) {
                result = false;
            }
        }
        return result;
    }

}
