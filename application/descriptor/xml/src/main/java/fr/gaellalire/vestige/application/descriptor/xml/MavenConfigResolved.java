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

package fr.gaellalire.vestige.application.descriptor.xml;

import java.util.Collections;
import java.util.List;

import fr.gaellalire.vestige.resolver.maven.DefaultDependencyModifier;
import fr.gaellalire.vestige.resolver.maven.DefaultJPMSConfiguration;
import fr.gaellalire.vestige.resolver.maven.MavenRepository;

/**
 * @author Gael Lalire
 */
public class MavenConfigResolved {

    private boolean superPomRepositoriesUsed;

    private boolean pomRepositoriesIgnored;

    private List<MavenRepository> additionalRepositories;

    private DefaultDependencyModifier defaultDependencyModifier;

    private DefaultJPMSConfiguration defaultJPMSConfiguration;

    public MavenConfigResolved() {
        superPomRepositoriesUsed = true;
        pomRepositoriesIgnored = false;
        additionalRepositories = Collections.emptyList();
        defaultDependencyModifier = new DefaultDependencyModifier();
        defaultJPMSConfiguration = new DefaultJPMSConfiguration();
    }

    public MavenConfigResolved(final boolean superPomRepositoriesUsed, final boolean pomRepositoriesIgnored, final List<MavenRepository> additionalRepositories,
            final DefaultDependencyModifier defaultDependencyModifier, final DefaultJPMSConfiguration defaultJPMSConfiguration) {
        this.superPomRepositoriesUsed = superPomRepositoriesUsed;
        this.pomRepositoriesIgnored = pomRepositoriesIgnored;
        this.additionalRepositories = additionalRepositories;
        this.defaultDependencyModifier = defaultDependencyModifier;
        this.defaultJPMSConfiguration = defaultJPMSConfiguration;
    }

    public boolean isSuperPomRepositoriesUsed() {
        return superPomRepositoriesUsed;
    }

    public boolean isPomRepositoriesIgnored() {
        return pomRepositoriesIgnored;
    }

    public List<MavenRepository> getAdditionalRepositories() {
        return additionalRepositories;
    }

    public DefaultDependencyModifier getDefaultDependencyModifier() {
        return defaultDependencyModifier;
    }

    public DefaultJPMSConfiguration getDefaultJPMSConfiguration() {
        return defaultJPMSConfiguration;
    }

}
