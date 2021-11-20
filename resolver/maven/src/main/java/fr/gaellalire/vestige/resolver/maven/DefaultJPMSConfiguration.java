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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import fr.gaellalire.vestige.platform.JPMSClassLoaderConfiguration;
import fr.gaellalire.vestige.platform.ModuleConfiguration;

/**
 * @author Gael Lalire
 */
public class DefaultJPMSConfiguration {

    private Map<MavenArtifactKey, JPMSClassLoaderConfiguration> moduleConfigurationByArtifactIdByGroupdId = new HashMap<MavenArtifactKey, JPMSClassLoaderConfiguration>();

    public void addModuleConfiguration(final MavenArtifactKey mavenArtifactKey, final Collection<ModuleConfiguration> moduleConfigurations) {
        moduleConfigurationByArtifactIdByGroupdId.put(mavenArtifactKey, JPMSClassLoaderConfiguration.EMPTY_INSTANCE.merge(moduleConfigurations));
    }

    public JPMSClassLoaderConfiguration getModuleConfiguration(final MavenArtifactKey mavenArtifactKey) {
        JPMSClassLoaderConfiguration moduleConfiguration = moduleConfigurationByArtifactIdByGroupdId.get(mavenArtifactKey);
        if (moduleConfiguration == null) {
            return JPMSClassLoaderConfiguration.EMPTY_INSTANCE;
        }
        return moduleConfiguration;
    }

}
