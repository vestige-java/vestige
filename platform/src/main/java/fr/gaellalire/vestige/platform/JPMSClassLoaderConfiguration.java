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

package fr.gaellalire.vestige.platform;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * @author gaellalire
 */
public final class JPMSClassLoaderConfiguration implements Serializable {

    private static final long serialVersionUID = -4958423018213646455L;

    public static final JPMSClassLoaderConfiguration EMPTY_INSTANCE = new JPMSClassLoaderConfiguration(Collections.<ModuleConfiguration> emptySet());

    private Set<ModuleConfiguration> moduleConfigurations;

    private JPMSClassLoaderConfiguration(final Set<ModuleConfiguration> moduleConfigurations) {
        this.moduleConfigurations = moduleConfigurations;
    }

    public Set<ModuleConfiguration> getModuleConfigurations() {
        return moduleConfigurations;
    }

    public JPMSClassLoaderConfiguration merge(final Collection<ModuleConfiguration> moduleConfigurations) {
        if (this == EMPTY_INSTANCE && moduleConfigurations.size() == 0) {
            return EMPTY_INSTANCE;
        }
        TreeMap<String, ModuleConfiguration> moduleConfigurationByModuleName = new TreeMap<String, ModuleConfiguration>();
        for (ModuleConfiguration moduleConfiguration : this.moduleConfigurations) {
            moduleConfigurationByModuleName.put(moduleConfiguration.getModuleName(), moduleConfiguration);
        }
        for (ModuleConfiguration moduleConfiguration : moduleConfigurations) {
            String moduleName = moduleConfiguration.getModuleName();
            ModuleConfiguration put = moduleConfigurationByModuleName.put(moduleName, moduleConfiguration);
            if (put != null) {
                // merge
                Set<String> addExports = new TreeSet<String>(moduleConfiguration.getAddExports());
                Set<String> addOpens = new TreeSet<String>(moduleConfiguration.getAddOpens());
                addExports.addAll(put.getAddExports());
                addOpens.addAll(put.getAddOpens());
                addExports.removeAll(addOpens);
                ModuleConfiguration merge = new ModuleConfiguration(moduleName, addExports, addOpens);
                moduleConfigurationByModuleName.put(moduleName, merge);
            }
        }
        return new JPMSClassLoaderConfiguration(new TreeSet<ModuleConfiguration>(moduleConfigurationByModuleName.values()));
    }

    public JPMSClassLoaderConfiguration merge(final JPMSClassLoaderConfiguration jpmsConfiguration) {
        return merge(jpmsConfiguration.moduleConfigurations);
    }

    @Override
    public int hashCode() {
        return moduleConfigurations.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof JPMSClassLoaderConfiguration)) {
            return false;
        }
        JPMSClassLoaderConfiguration other = (JPMSClassLoaderConfiguration) obj;
        if (moduleConfigurations == null) {
            if (other.moduleConfigurations != null) {
                return false;
            }
        } else if (!moduleConfigurations.equals(other.moduleConfigurations)) {
            return false;
        }
        return true;
    }

}
