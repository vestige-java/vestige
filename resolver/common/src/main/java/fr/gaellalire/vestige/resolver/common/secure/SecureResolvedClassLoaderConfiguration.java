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

package fr.gaellalire.vestige.resolver.common.secure;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.security.Permission;
import java.util.Collection;

import fr.gaellalire.vestige.spi.resolver.AttachedClassLoader;
import fr.gaellalire.vestige.spi.resolver.PartiallyVerifiedAttachment;
import fr.gaellalire.vestige.spi.resolver.ResolvedClassLoaderConfiguration;
import fr.gaellalire.vestige.spi.resolver.ResolverException;
import fr.gaellalire.vestige.spi.system.VestigeSystem;

/**
 * @author Gael Lalire
 */
public class SecureResolvedClassLoaderConfiguration implements ResolvedClassLoaderConfiguration {

    private VestigeSystem secureVestigeSystem;

    private ResolvedClassLoaderConfiguration delegate;

    public SecureResolvedClassLoaderConfiguration(final VestigeSystem secureVestigeSystem, final ResolvedClassLoaderConfiguration delegate) {
        this.secureVestigeSystem = secureVestigeSystem;
        this.delegate = delegate;
    }

    @Override
    public AttachedClassLoader attach() throws ResolverException, InterruptedException {
        VestigeSystem vestigeSystem = secureVestigeSystem.setCurrentSystem();
        try {
            return new SecureAttachedClassLoader(secureVestigeSystem, delegate.attach());
        } finally {
            vestigeSystem.setCurrentSystem();
        }
    }

    @Override
    public void save(final ObjectOutputStream objectOutputStream) throws IOException {
        delegate.save(objectOutputStream);
    }

    @Override
    public Collection<Permission> getPermissions() {
        return delegate.getPermissions();
    }

    @Override
    public boolean isAttachmentScoped() {
        return delegate.isAttachmentScoped();
    }

    @Override
    public String createVerificationMetadata() throws ResolverException {
        VestigeSystem vestigeSystem = secureVestigeSystem.setCurrentSystem();
        try {
            return delegate.createVerificationMetadata();
        } finally {
            vestigeSystem.setCurrentSystem();
        }
    }

    @Override
    public AttachedClassLoader verifiedAttach(final String signature) throws ResolverException, InterruptedException {
        VestigeSystem vestigeSystem = secureVestigeSystem.setCurrentSystem();
        try {
            return new SecureAttachedClassLoader(secureVestigeSystem, delegate.verifiedAttach(signature));
        } finally {
            vestigeSystem.setCurrentSystem();
        }
    }

    @Override
    public PartiallyVerifiedAttachment partiallyVerifiedAttach(final String signature) throws ResolverException, InterruptedException {
        VestigeSystem vestigeSystem = secureVestigeSystem.setCurrentSystem();
        try {
            return delegate.partiallyVerifiedAttach(signature);
        } finally {
            vestigeSystem.setCurrentSystem();
        }
    }

}
