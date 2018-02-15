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

package fr.gaellalire.vestige.application.manager.test;

import java.io.IOException;
import java.net.URL;

/**
 * @author Gael Lalire
 */
public class MyClassLoader extends ClassLoader {

    public static final String ITF_NAME = Itf.class.getName();

    private static final String ITF_RESOURCE_NAME = ITF_NAME.replace('.', '/').concat(".class");

    public static final String ITF2_NAME = Itf2.class.getName();

    private static final String ITF2_RESOURCE_NAME = ITF2_NAME.replace('.', '/').concat(".class");

    public static final String EXCEPTION_NAME = MyException.class.getName();

    private static final String EXCEPTION_RESOURCE_NAME = EXCEPTION_NAME.replace('.', '/').concat(".class");

    private String name;

    public MyClassLoader(final String name) {
        this.name = name;
    }

    @Override
    public Class<?> loadClass(final String name) throws ClassNotFoundException {
        URL resource = null;
        if (ITF_NAME.equals(name)) {
            resource = TestProxy.class.getResource("/" + ITF_RESOURCE_NAME);
        } else if (ITF2_NAME.equals(name)) {
            resource = TestProxy.class.getResource("/" + ITF2_RESOURCE_NAME);
        } else if (EXCEPTION_NAME.equals(name)) {
            resource = TestProxy.class.getResource("/" + EXCEPTION_RESOURCE_NAME);
        }
        if (resource != null) {
            byte[] b = new byte[2048];
            int l;
            try {
                l = resource.openStream().read(b);
            } catch (IOException e) {
                throw new ClassNotFoundException();
            }
            return defineClass(name, b, 0, l);
        }
        return super.loadClass(name);
    }

    @Override
    public String toString() {
        return name;
    }

}
