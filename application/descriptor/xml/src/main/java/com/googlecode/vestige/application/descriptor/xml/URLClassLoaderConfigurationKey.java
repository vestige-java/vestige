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

package com.googlecode.vestige.application.descriptor.xml;

import java.io.Serializable;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

/**
 * @author Gael Lalire
 */
public class URLClassLoaderConfigurationKey implements Serializable {

    private static final long serialVersionUID = 3390959845313644407L;

    private List<URL> urls;

    private boolean shared;

    public URLClassLoaderConfigurationKey(final boolean shared, final URL... urls) {
        this.shared = shared;
        this.urls = Arrays.asList(urls);
    }

    @Override
    public int hashCode() {
        return urls.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!shared) {
            return false;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof URLClassLoaderConfigurationKey)) {
            return false;
        }
        URLClassLoaderConfigurationKey other = (URLClassLoaderConfigurationKey) obj;
        if (!other.shared) {
            return false;
        }
        if (!urls.equals(other.urls)) {
            return false;
        }
        return true;
    }

}
