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

package fr.gaellalire.vestige.resolver.common;

import java.lang.ref.SoftReference;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import fr.gaellalire.vestige.core.VestigeClassLoader;
import fr.gaellalire.vestige.platform.AttachedVestigeClassLoader;
import fr.gaellalire.vestige.platform.VestigePlatform;
import fr.gaellalire.vestige.spi.resolver.AttachableClassLoader;
import fr.gaellalire.vestige.spi.resolver.AttachedClassLoader;
import fr.gaellalire.vestige.spi.resolver.VestigeJar;

/**
 * @author Gael Lalire
 */
public class DefaultAttachableClassLoader implements AttachableClassLoader {

    private VestigePlatform vestigePlatform;

    private VestigeClassLoader<AttachedVestigeClassLoader> classLoader;

    private List<VestigeJar> vestigeJars;

    public DefaultAttachableClassLoader(final VestigePlatform vestigePlatform, final VestigeClassLoader<AttachedVestigeClassLoader> classLoader,
            final List<VestigeJar> vestigeJars) {
        this.vestigePlatform = vestigePlatform;
        this.classLoader = classLoader;
        this.vestigeJars = vestigeJars;
    }

    @Override
    public AttachedClassLoader attach() throws InterruptedException {
        int installerAttach;
        synchronized (vestigePlatform) {
            installerAttach = vestigePlatform.attach(classLoader);
        }
        return new DefaultAttachedClassLoader(vestigePlatform, installerAttach, this);
    }

    public VestigeClassLoader<AttachedVestigeClassLoader> getClassLoader() {
        return classLoader;
    }

    public void addSoftReferenceObject(final Object dataObject) {
        synchronized (vestigePlatform) {
            classLoader.getData(vestigePlatform).getSoftReferences().add(new SoftReference<Object>(dataObject));
        }
    }

    public Enumeration<? extends VestigeJar> getVestigeJarEnumeration() {
        return new Enumeration<VestigeJar>() {

            private Iterator<VestigeJar> vestigeJarsIterator = vestigeJars.iterator();

            @Override
            public boolean hasMoreElements() {
                return vestigeJarsIterator.hasNext();
            }

            @Override
            public VestigeJar nextElement() {
                return vestigeJarsIterator.next();
            }
        };
    }

}
