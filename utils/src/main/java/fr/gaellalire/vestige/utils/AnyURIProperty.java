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

package fr.gaellalire.vestige.utils;

/**
 * @author Gael Lalire
 */
public class AnyURIProperty extends Property<String> {

    private static final long serialVersionUID = -5559543021914822495L;

    public AnyURIProperty(final String rawValue) {
        super(rawValue);
    }

    @Override
    public String convert(final String value) {
        return value;
    }

}
