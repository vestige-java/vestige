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

package com.googlecode.vestige.resolver.maven;

import java.io.Serializable;
import java.util.List;

import com.googlecode.vestige.platform.ClassLoaderConfiguration;

/**
 * @author Gael Lalire
 */
public class MavenResolverCache implements Serializable {

    private static final long serialVersionUID = 561193783052759466L;

    private List<ClassLoaderConfiguration> launchCaches;

    private String className;

    private ClassLoaderConfiguration classLoaderConfiguration;

    private long lastModified;

    public MavenResolverCache(final List<ClassLoaderConfiguration> classLoaderConfigurations, final String className, final ClassLoaderConfiguration classLoaderConfiguration, final long lastModified) {
        this.launchCaches = classLoaderConfigurations;
        this.className = className;
        this.classLoaderConfiguration = classLoaderConfiguration;
        this.lastModified = lastModified;
    }

    public List<ClassLoaderConfiguration> getClassLoaderConfigurations() {
        return launchCaches;
    }

    public long getLastModified() {
        return lastModified;
    }

    public String getClassName() {
        return className;
    }

    public ClassLoaderConfiguration getClassLoaderConfiguration() {
        return classLoaderConfiguration;
    }

}
