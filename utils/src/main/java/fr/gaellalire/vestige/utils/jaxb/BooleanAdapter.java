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

package fr.gaellalire.vestige.utils.jaxb;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import fr.gaellalire.vestige.utils.BooleanProperty;

/**
 * @author Gael Lalire
 */
public class BooleanAdapter extends XmlAdapter<String, BooleanProperty> {

    @Override
    public BooleanProperty unmarshal(final String v) {
        return new BooleanProperty(v);
    }

    @Override
    public String marshal(final BooleanProperty v) {
        return v.getRawValue();
    }

}
