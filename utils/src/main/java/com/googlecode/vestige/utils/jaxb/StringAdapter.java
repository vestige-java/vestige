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

package com.googlecode.vestige.utils.jaxb;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import com.googlecode.vestige.utils.PropertyExpander;

/**
 * @author Gael Lalire
 */
public class StringAdapter extends XmlAdapter<String, String> {

    @Override
    public String unmarshal(final String v) throws Exception {
        return PropertyExpander.expand(v);
    }

    @Override
    public String marshal(final String v) throws Exception {
        return v;
    }

}
