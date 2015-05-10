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

/**
 * @author Gael Lalire
 */
public class MavenRepository {

    private String id;

    private String layout;

    private String url;

    public MavenRepository(final String id, final String layout, final String url) {
        this.id = id;
        if (layout == null) {
            this.layout = "default";
        } else {
            this.layout = layout;
        }
        this.url = url;
    }

    public String getId() {
        return id;
    }

    public String getLayout() {
        return layout;
    }

    public String getUrl() {
        return url;
    }

}
