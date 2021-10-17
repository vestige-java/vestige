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

import java.util.Enumeration;

import fr.gaellalire.vestige.resolver.common.secure.SecureResolvedClassLoaderConfiguration;
import fr.gaellalire.vestige.spi.resolver.maven.MavenArtifact;
import fr.gaellalire.vestige.spi.resolver.maven.MavenResolvedClassLoaderConfiguration;
import fr.gaellalire.vestige.spi.system.VestigeSystem;

/**
 * @author Gael Lalire
 */
public class SecureMavenResolvedClassLoaderConfiguration extends SecureResolvedClassLoaderConfiguration<MavenResolvedClassLoaderConfiguration>
        implements MavenResolvedClassLoaderConfiguration {

    public SecureMavenResolvedClassLoaderConfiguration(final VestigeSystem secureVestigeSystem, final MavenResolvedClassLoaderConfiguration delegate) {
        super(secureVestigeSystem, delegate);
    }

    @Override
    public Enumeration<MavenArtifact> getUsedArtifacts() {
        MavenResolvedClassLoaderConfiguration delegate = getDelegate();
        return delegate.getUsedArtifacts();
    }

}
