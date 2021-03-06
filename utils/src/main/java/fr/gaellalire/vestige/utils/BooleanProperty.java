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
public class BooleanProperty extends Property<Boolean> {

    private static final long serialVersionUID = -6574145998056432239L;

    public BooleanProperty(final String rawValue) {
        super(rawValue);
    }

    @Override
    public Boolean convert(final String value) {
        return Boolean.parseBoolean(value);
    }

}
