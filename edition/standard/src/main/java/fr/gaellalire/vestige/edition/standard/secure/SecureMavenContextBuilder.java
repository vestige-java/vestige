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

package fr.gaellalire.vestige.edition.standard.secure;

import fr.gaellalire.vestige.spi.resolver.Scope;
import fr.gaellalire.vestige.spi.resolver.maven.MavenContext;
import fr.gaellalire.vestige.spi.resolver.maven.MavenContextBuilder;
import fr.gaellalire.vestige.spi.resolver.maven.ModifyDependencyRequest;
import fr.gaellalire.vestige.spi.resolver.maven.ReplaceDependencyRequest;
import fr.gaellalire.vestige.spi.resolver.maven.ResolveMavenArtifactRequest;
import fr.gaellalire.vestige.spi.resolver.maven.ResolveMode;
import fr.gaellalire.vestige.spi.system.VestigeSystem;

/**
 * @author Gael Lalire
 */
public class SecureMavenContextBuilder implements MavenContextBuilder, MavenContext {

    private VestigeSystem vestigeSystem;

    private MavenContextBuilder delegate;

    private MavenContext mavenContext;

    public SecureMavenContextBuilder(final VestigeSystem vestigeSystem, final MavenContextBuilder delegate) {
        this.vestigeSystem = vestigeSystem;
        this.delegate = delegate;
    }

    public SecureMavenContextBuilder(final VestigeSystem vestigeSystem, final MavenContext mavenContext) {
        this.vestigeSystem = vestigeSystem;
        this.mavenContext = mavenContext;
    }

    @Override
    public void addAdditionalRepository(final String id, final String layout, final String url) {
        delegate.addAdditionalRepository(id, layout, url);
    }

    @Override
    public ModifyDependencyRequest addModifyDependency(final String groupId, final String artifactId) {
        return delegate.addModifyDependency(groupId, artifactId);
    }

    @Override
    public ReplaceDependencyRequest addReplaceDependency(final String groupId, final String artifactId) {
        return delegate.addReplaceDependency(groupId, artifactId);
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
        return new SecureMavenContextBuilder(vestigeSystem, mavenContext);
    }

    @Override
    public ResolveMavenArtifactRequest resolve(final ResolveMode resolveMode, final Scope scope, final String groupId, final String artifactId, final String version,
            final String name) {
        // TODO Auto-generated method stub
        return null;
    }

}
