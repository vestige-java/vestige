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

package fr.gaellalire.vestige.resolver.maven;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import fr.gaellalire.vestige.core.executor.VestigeWorker;
import fr.gaellalire.vestige.platform.ClassLoaderConfiguration;
import fr.gaellalire.vestige.platform.VestigePlatform;
import fr.gaellalire.vestige.resolver.common.DefaultResolvedClassLoaderConfiguration;
import fr.gaellalire.vestige.spi.resolver.maven.MavenArtifact;
import fr.gaellalire.vestige.spi.resolver.maven.MavenResolvedClassLoaderConfiguration;

/**
 * @author Gael Lalire
 */
public class DefaultMavenResolvedClassLoaderConfiguration extends DefaultResolvedClassLoaderConfiguration implements MavenResolvedClassLoaderConfiguration {

    private List<DefaultMavenArtifact> usedArtifacts;

    public DefaultMavenResolvedClassLoaderConfiguration(final VestigePlatform vestigePlatform, final VestigeWorker vestigeWorker,
            final ClassLoaderConfiguration classLoaderConfiguration, final boolean firstBeforeParent, final List<DefaultMavenArtifact> usedArtifacts) {
        super(vestigePlatform, vestigeWorker, classLoaderConfiguration, firstBeforeParent);
        this.usedArtifacts = usedArtifacts;
    }

    @Override
    public void saveOtherFields(final ObjectOutputStream internObjectOutputStream) throws IOException {
        internObjectOutputStream.writeObject(usedArtifacts);
    }

    @Override
    public Enumeration<MavenArtifact> getUsedArtifacts() {
        final Iterator<DefaultMavenArtifact> iterator = usedArtifacts.iterator();
        return new Enumeration<MavenArtifact>() {

            @Override
            public boolean hasMoreElements() {
                return iterator.hasNext();
            }

            @Override
            public MavenArtifact nextElement() {
                return iterator.next();
            }
        };
    }

}
