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

package fr.gaellalire.vestige.resolver.maven.secure;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.security.Permission;
import java.security.PermissionCollection;

import fr.gaellalire.vestige.spi.resolver.maven.MavenContextBuilder;
import fr.gaellalire.vestige.spi.resolver.maven.MavenResolvedClassLoaderConfiguration;
import fr.gaellalire.vestige.spi.resolver.maven.VestigeMavenResolver;
import fr.gaellalire.vestige.spi.system.VestigeSystem;
import fr.gaellalire.vestige.system.PrivateVestigePolicy;

/**
 * @author Gael Lalire
 */
public class SecureVestigeMavenResolver implements VestigeMavenResolver {

    private VestigeSystem secureVestigeSystem;

    private PrivateVestigePolicy vestigePolicy;

    private VestigeMavenResolver delegate;

    public SecureVestigeMavenResolver(final VestigeSystem secureVestigeSystem, final PrivateVestigePolicy vestigePolicy, final VestigeMavenResolver delegate) {
        this.secureVestigeSystem = secureVestigeSystem;
        this.vestigePolicy = vestigePolicy;
        this.delegate = delegate;
    }

    @Override
    public MavenContextBuilder createMavenContextBuilder() {
        VestigeSystem vestigeSystem = secureVestigeSystem.setCurrentSystem();
        try {
            return new SecureMavenContextBuilder(secureVestigeSystem, vestigePolicy, delegate.createMavenContextBuilder());
        } finally {
            vestigeSystem.setCurrentSystem();
        }
    }

    @Override
    public MavenResolvedClassLoaderConfiguration restoreSavedResolvedClassLoaderConfiguration(final ObjectInputStream objectInputStream) throws IOException {
        VestigeSystem vestigeSystem = secureVestigeSystem.setCurrentSystem();
        try {
            MavenResolvedClassLoaderConfiguration resolvedClassLoaderConfiguration = delegate
                    .restoreSavedResolvedClassLoaderConfiguration(new SecureObjectInputStream(vestigeSystem, objectInputStream));
            PermissionCollection permissionCollection = vestigePolicy.getPermissionCollection();
            for (Permission permission : resolvedClassLoaderConfiguration.getPermissions()) {
                permissionCollection.add(permission);
            }
            return new SecureMavenResolvedClassLoaderConfiguration(secureVestigeSystem, resolvedClassLoaderConfiguration);
        } finally {
            vestigeSystem.setCurrentSystem();
        }
    }

    @Override
    public String toString() {
        return delegate.toString();
    }

}
