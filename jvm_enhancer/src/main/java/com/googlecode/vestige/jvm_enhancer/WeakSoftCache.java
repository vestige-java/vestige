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

package com.googlecode.vestige.jvm_enhancer;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * @author Gael Lalire
 */
@SuppressWarnings("restriction")
public class WeakSoftCache extends sun.misc.SoftCache {

    private Map<Object, Object> map = new WeakHashMap<Object, Object>();

    @Override
    public Object get(final Object arg0) {
        return map.get(arg0);
    }

    @Override
    public Object put(final Object arg0, final Object arg1) {
        return map.put(arg0, arg1);
    }

}
