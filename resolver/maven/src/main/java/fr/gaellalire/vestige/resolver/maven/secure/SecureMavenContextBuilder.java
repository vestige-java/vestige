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

import fr.gaellalire.vestige.spi.resolver.Scope;
import fr.gaellalire.vestige.spi.resolver.maven.MavenContext;
import fr.gaellalire.vestige.spi.resolver.maven.MavenContextBuilder;
import fr.gaellalire.vestige.spi.resolver.maven.ModifyDependencyRequest;
import fr.gaellalire.vestige.spi.resolver.maven.ReplaceDependencyRequest;
import fr.gaellalire.vestige.spi.resolver.maven.ResolveMavenArtifactRequest;
import fr.gaellalire.vestige.spi.resolver.maven.ResolveMode;
import fr.gaellalire.vestige.spi.system.VestigeSystem;
import fr.gaellalire.vestige.system.PrivateVestigePolicy;

/**
 * @author Gael Lalire
 */
public class SecureMavenContextBuilder implements MavenContextBuilder, MavenContext {

    private VestigeSystem secureVestigeSystem;

    private PrivateVestigePolicy vestigePolicy;

    private MavenContextBuilder delegate;

    private MavenContext mavenContext;

    public SecureMavenContextBuilder(final VestigeSystem secureVestigeSystem, final PrivateVestigePolicy vestigePolicy, final MavenContextBuilder delegate) {
        this.secureVestigeSystem = secureVestigeSystem;
        this.vestigePolicy = vestigePolicy;
        this.delegate = delegate;
    }

    public SecureMavenContextBuilder(final VestigeSystem secureVestigeSystem, final PrivateVestigePolicy vestigePolicy, final MavenContext mavenContext) {
        this.secureVestigeSystem = secureVestigeSystem;
        this.vestigePolicy = vestigePolicy;
        this.mavenContext = mavenContext;
    }

    @Override
    public void addAdditionalRepository(final String id, final String layout, final String url) {
        VestigeSystem vestigeSystem = secureVestigeSystem.setCurrentSystem();
        try {
            delegate.addAdditionalRepository(id, layout, url);
        } finally {
            vestigeSystem.setCurrentSystem();
        }
    }

    @Override
    public ModifyDependencyRequest addModifyDependency(final String groupId, final String artifactId) {
        VestigeSystem vestigeSystem = secureVestigeSystem.setCurrentSystem();
        try {
            return delegate.addModifyDependency(groupId, artifactId);
        } finally {
            vestigeSystem.setCurrentSystem();
        }
    }

    @Override
    public ReplaceDependencyRequest addReplaceDependency(final String groupId, final String artifactId) {
        VestigeSystem vestigeSystem = secureVestigeSystem.setCurrentSystem();
        try {
            return delegate.addReplaceDependency(groupId, artifactId);
        } finally {
            vestigeSystem.setCurrentSystem();
        }
    }

    @Override
    public void setSuperPomRepositoriesUsed(final boolean superPomRepositoriesUsed) {
        delegate.setSuperPomRepositoriesUsed(superPomRepositoriesUsed);
    }

    @Override
    public void setPomRepositoriesIgnored(final boolean pomRepositoriesIgnored) {
        delegate.setPomRepositoriesIgnored(pomRepositoriesIgnored);
    }

    @Override
    public MavenContext build() {
        mavenContext = delegate.build();
        if (mavenContext == delegate) {
            return this;
        }
        return new SecureMavenContextBuilder(secureVestigeSystem, vestigePolicy, mavenContext);
    }

    @Override
    public ResolveMavenArtifactRequest resolve(final ResolveMode resolveMode, final Scope scope, final String groupId, final String artifactId, final String version,
            final String name) {
        return resolve(resolveMode, scope, groupId, artifactId, version, "jar", name);
    }

    @Override
    public ResolveMavenArtifactRequest resolve(final ResolveMode resolveMode, final Scope scope, final String groupId, final String artifactId, final String version,
            final String extension, final String name) {
        VestigeSystem vestigeSystem = secureVestigeSystem.setCurrentSystem();
        try {
            return new SecureResolveMavenArtifactRequest(secureVestigeSystem, vestigePolicy,
                    mavenContext.resolve(resolveMode, scope, groupId, artifactId, version, extension, name));
        } finally {
            vestigeSystem.setCurrentSystem();
        }
    }

}
