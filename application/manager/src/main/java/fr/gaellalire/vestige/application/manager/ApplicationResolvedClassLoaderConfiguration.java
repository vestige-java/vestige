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

package fr.gaellalire.vestige.application.manager;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.security.Permission;
import java.util.Collection;
import java.util.Enumeration;

import fr.gaellalire.vestige.spi.resolver.AttachedClassLoader;
import fr.gaellalire.vestige.spi.resolver.ResolvedClassLoaderConfiguration;
import fr.gaellalire.vestige.spi.resolver.ResolverException;
import fr.gaellalire.vestige.spi.resolver.VestigeJar;

/**
 * @author Gael Lalire
 */
public class ApplicationResolvedClassLoaderConfiguration implements ResolvedClassLoaderConfiguration {

    private ResolvedClassLoaderConfiguration delegate;

    private int resolverIndex;

    public ApplicationResolvedClassLoaderConfiguration(final ResolvedClassLoaderConfiguration delegate, final int resolverIndex) {
        this.delegate = delegate;
        this.resolverIndex = resolverIndex;
    }

    @Override
    public AttachedClassLoader attach() throws ResolverException, InterruptedException {
        return delegate.attach();
    }

    @Override
    public void save(final ObjectOutputStream objectOutputStream) throws IOException {
        delegate.save(objectOutputStream);
    }

    @Override
    public Collection<Permission> getPermissions() {
        return delegate.getPermissions();
    }

    public boolean isAttachmentScoped() {
        return delegate.isAttachmentScoped();
    }

    public int getResolverIndex() {
        return resolverIndex;
    }

    @Override
    public String toString() {
        return delegate.toString();
    }

    @Override
    public Enumeration<? extends VestigeJar> getVestigeJarEnumeration() {
        return delegate.getVestigeJarEnumeration();
    }

    @Override
    public String createVerificationMetadata() throws ResolverException {
        return delegate.createVerificationMetadata();
    }

    @Override
    public AttachedClassLoader verifiedAttach(final String signature) throws ResolverException, InterruptedException {
        return delegate.verifiedAttach(signature);
    }

}
