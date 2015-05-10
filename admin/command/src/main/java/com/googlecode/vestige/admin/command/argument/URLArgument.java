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

package com.googlecode.vestige.admin.command.argument;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;

/**
 * @author Gael Lalire
 */
public class URLArgument implements Argument {

    private static final String NAME = "<url>";

    public String getName() {
        return NAME;
    }

    private boolean directory;

    private URL url;

    public URLArgument(final boolean directory) {
        this.directory = directory;
    }

    public URL getUrl() {
        return url;
    }

    public void parse(final String s) throws ParseException {
        try {
            if (directory && !s.endsWith("/")) {
                url = new URL(s + "/");
            } else {
                url = new URL(s);
            }
        } catch (MalformedURLException e) {
            throw new ParseException(e);
        }
    }

    public Collection<String> propose() {
        return null;
    }

    public void reset() {
        url = null;
    }

}
