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

import fr.gaellalire.vestige.job.secure.SecureJobHelper;
import fr.gaellalire.vestige.spi.job.JobHelper;
import fr.gaellalire.vestige.spi.resolver.ResolverException;
import fr.gaellalire.vestige.spi.resolver.maven.ResolveMavenArtifactRequest;
import fr.gaellalire.vestige.spi.resolver.maven.ResolvedMavenArtifact;
import fr.gaellalire.vestige.spi.system.VestigeSystem;
import fr.gaellalire.vestige.system.PrivateVestigePolicy;

/**
 * @author Gael Lalire
 */
public class SecureResolveMavenArtifactRequest implements ResolveMavenArtifactRequest {

    private VestigeSystem secureVestigeSystem;

    private PrivateVestigePolicy vestigePolicy;

    private ResolveMavenArtifactRequest delegate;

    public SecureResolveMavenArtifactRequest(final VestigeSystem secureVestigeSystem, final PrivateVestigePolicy vestigePolicy, final ResolveMavenArtifactRequest delegate) {
        this.secureVestigeSystem = secureVestigeSystem;
        this.vestigePolicy = vestigePolicy;
        this.delegate = delegate;
    }

    @Override
    public void setExtension(final String extension) {
        delegate.setExtension(extension);
    }

    @Override
    public ResolvedMavenArtifact execute(final JobHelper jobHelper) throws ResolverException {
        VestigeSystem vestigeSystem = secureVestigeSystem.setCurrentSystem();
        try {
            ResolvedMavenArtifact execute = delegate.execute(new SecureJobHelper(vestigeSystem, jobHelper));
            return new SecureResolvedMavenArtifact(secureVestigeSystem, vestigePolicy, execute);
        } finally {
            vestigeSystem.setCurrentSystem();
        }
    }

    @Override
    public String toString() {
        return delegate.toString();
    }

}
