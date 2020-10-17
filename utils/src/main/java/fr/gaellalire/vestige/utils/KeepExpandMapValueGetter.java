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

import java.util.HashMap;
import java.util.Map;

/**
 * @author Gael Lalire
 */
public class KeepExpandMapValueGetter implements ValueGetter {

    private Map<String, String> expandMap = new HashMap<String, String>();

    @Override
    public <E> E getValue(final Property<E> property) {
        if (property == null) {
            return null;
        }
        Map<String, String> propertyExpandMap = property.getExpandMap();
        if (propertyExpandMap != null) {
            expandMap.putAll(propertyExpandMap);
        }
        return property.getValue();
    }

    public Map<String, String> getExpandMap() {
        return expandMap;
    }

}
