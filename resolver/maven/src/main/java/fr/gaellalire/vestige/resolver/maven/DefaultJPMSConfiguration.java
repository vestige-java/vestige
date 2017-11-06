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
 * @author gaellalire
 */
public class DefaultJPMSConfiguration {

    private Map<String, Map<String, JPMSClassLoaderConfiguration>> moduleConfigurationByArtifactIdByGroupdId = new HashMap<String, Map<String, JPMSClassLoaderConfiguration>>();

    public void addModuleConfiguration(final String groupId, final String artifactId, final Collection<ModuleConfiguration> moduleConfigurations) {
        Map<String, JPMSClassLoaderConfiguration> moduleConfigurationByArtifactId = moduleConfigurationByArtifactIdByGroupdId.get(groupId);
        if (moduleConfigurationByArtifactId == null) {
            moduleConfigurationByArtifactId = new HashMap<String, JPMSClassLoaderConfiguration>();
            moduleConfigurationByArtifactIdByGroupdId.put(groupId, moduleConfigurationByArtifactId);
        }
        moduleConfigurationByArtifactId.put(artifactId, JPMSClassLoaderConfiguration.EMPTY_INSTANCE.merge(moduleConfigurations));
    }

    public JPMSClassLoaderConfiguration getModuleConfiguration(final String groupId, final String artifactId) {
        Map<String, JPMSClassLoaderConfiguration> moduleConfigurationByArtifactId = moduleConfigurationByArtifactIdByGroupdId.get(groupId);
        if (moduleConfigurationByArtifactId == null) {
            return JPMSClassLoaderConfiguration.EMPTY_INSTANCE;
        }
        JPMSClassLoaderConfiguration moduleConfiguration = moduleConfigurationByArtifactId.get(artifactId);
        if (moduleConfiguration == null) {
            return JPMSClassLoaderConfiguration.EMPTY_INSTANCE;
        }
        return moduleConfiguration;
    }

}
