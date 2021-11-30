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

import java.io.File;
import java.util.Enumeration;

import fr.gaellalire.vestige.resolver.common.secure.ElementSecureMaker;
import fr.gaellalire.vestige.resolver.common.secure.SecureEnumeration;
import fr.gaellalire.vestige.spi.resolver.ResolverException;
import fr.gaellalire.vestige.spi.resolver.Scope;
import fr.gaellalire.vestige.spi.resolver.maven.CreateClassLoaderConfigurationRequest;
import fr.gaellalire.vestige.spi.resolver.maven.ResolveMode;
import fr.gaellalire.vestige.spi.resolver.maven.ResolvedMavenArtifact;
import fr.gaellalire.vestige.spi.system.VestigeSystem;
import fr.gaellalire.vestige.system.PrivateVestigePolicy;

/**
 * @author Gael Lalire
 */
public class SecureResolvedMavenArtifact implements ResolvedMavenArtifact {

    private VestigeSystem secureVestigeSystem;

    private PrivateVestigePolicy vestigePolicy;

    private ResolvedMavenArtifact delegate;

    public SecureResolvedMavenArtifact(final VestigeSystem secureVestigeSystem, final PrivateVestigePolicy vestigePolicy, final ResolvedMavenArtifact delegate) {
        this.secureVestigeSystem = secureVestigeSystem;
        this.vestigePolicy = vestigePolicy;
        this.delegate = delegate;
    }

    @Override
    public String getGroupId() {
        return delegate.getGroupId();
    }

    @Override
    public String getArtifactId() {
        return delegate.getArtifactId();
    }

    @Override
    public String getVersion() {
        return delegate.getVersion();
    }

    @Override
    public String getExtension() {
        return delegate.getExtension();
    }

    @Override
    public String getClassifier() {
        return delegate.getClassifier();
    }

    @Override
    public Enumeration<ResolvedMavenArtifact> getDependencies() throws ResolverException {
        VestigeSystem vestigeSystem = secureVestigeSystem.setCurrentSystem();
        try {
            return new SecureEnumeration<ResolvedMavenArtifact>(vestigeSystem, new ElementSecureMaker<ResolvedMavenArtifact>() {

                @Override
                public ResolvedMavenArtifact makeSecure(final ResolvedMavenArtifact e) {
                    return new SecureResolvedMavenArtifact(secureVestigeSystem, vestigePolicy, e);
                }
            }, delegate.getDependencies());
        } finally {
            vestigeSystem.setCurrentSystem();
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public File getFile() {
        return delegate.getFile();
    }

    @Override
    public CreateClassLoaderConfigurationRequest createClassLoaderConfiguration(final String name, final ResolveMode mode, final Scope scope) {
        VestigeSystem vestigeSystem = secureVestigeSystem.setCurrentSystem();
        try {
            return new SecureCreateClassLoaderConfigurationRequest(secureVestigeSystem, vestigePolicy, delegate.createClassLoaderConfiguration(name, mode, scope));
        } finally {
            vestigeSystem.setCurrentSystem();
        }
    }

    @Override
    public String toString() {
        return delegate.toString();
    }

}
