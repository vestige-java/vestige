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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.security.Permission;
import java.util.Collection;
import java.util.Enumeration;

import fr.gaellalire.vestige.platform.ClassLoaderConfiguration;
import fr.gaellalire.vestige.platform.VestigePlatform;
import fr.gaellalire.vestige.spi.resolver.AttachedClassLoader;
import fr.gaellalire.vestige.spi.resolver.ResolvedClassLoaderConfiguration;
import fr.gaellalire.vestige.spi.resolver.ResolverException;
import fr.gaellalire.vestige.spi.resolver.VestigeJar;

/**
 * @author Gael Lalire
 */
public class DefaultResolvedClassLoaderConfiguration implements ResolvedClassLoaderConfiguration {

    private VestigePlatform vestigePlatform;

    private ClassLoaderConfiguration classLoaderConfiguration;

    private boolean firstBeforeParent;

    public DefaultResolvedClassLoaderConfiguration(final VestigePlatform vestigePlatform, final ClassLoaderConfiguration classLoaderConfiguration,
            final boolean firstBeforeParent) {
        this.vestigePlatform = vestigePlatform;
        this.classLoaderConfiguration = classLoaderConfiguration;
    }

    @Override
    public void save(final ObjectOutputStream objectOutputStream) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ObjectOutputStream internObjectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
        try {
            internObjectOutputStream.writeBoolean(firstBeforeParent);
            internObjectOutputStream.writeObject(classLoaderConfiguration);
        } finally {
            internObjectOutputStream.close();
        }
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        objectOutputStream.writeInt(byteArray.length);
        objectOutputStream.write(byteArray, 0, byteArray.length);
    }

    @Override
    public AttachedClassLoader attach() throws ResolverException, InterruptedException {
        int installerAttach;
        synchronized (vestigePlatform) {
            try {
                installerAttach = vestigePlatform.attach(classLoaderConfiguration);
            } catch (IOException e) {
                throw new ResolverException("Unable to attach", e);
            }
        }
        return new DefaultAttachedClassLoader(vestigePlatform, installerAttach, new DefaultAttachableClassLoader(vestigePlatform, vestigePlatform.getClassLoader(installerAttach)));
    }

    @Override
    public Collection<Permission> getPermissions() {
        return classLoaderConfiguration.getPermissions();
    }

    @Override
    public boolean isAttachmentScoped() {
        return classLoaderConfiguration.isAttachmentScoped();
    }

    @Override
    public String toString() {
        return classLoaderConfiguration.toString();
    }

    @Override
    public Enumeration<? extends VestigeJar> getVestigeJarEnumeration() {
        final DefaultVestigeJarContext defaultVestigeJarContext = new DefaultVestigeJarContext(classLoaderConfiguration, firstBeforeParent);
        return new Enumeration<VestigeJar>() {

            @Override
            public boolean hasMoreElements() {
                return defaultVestigeJarContext.hasNext();
            }

            @Override
            public VestigeJar nextElement() {
                return new DefaultVestigeJar(defaultVestigeJarContext.next());
            }
        };
    }

}
