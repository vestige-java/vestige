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
import java.util.Set;

/**
 * @author gaellalire
 */
public final class ModuleConfiguration implements Serializable, Comparable<ModuleConfiguration> {

    private static final long serialVersionUID = 7556651787547224769L;

    private String moduleName;

    private Set<String> addExports;

    private Set<String> addOpens;

    public ModuleConfiguration(final String moduleName, final Set<String> addExports, final Set<String> addOpens) {
        this.moduleName = moduleName;
        this.addExports = addExports;
        this.addOpens = addOpens;
    }

    public String getModuleName() {
        return moduleName;
    }

    public Set<String> getAddExports() {
        return addExports;
    }

    public Set<String> getAddOpens() {
        return addOpens;
    }

    @Override
    public int hashCode() {
        return moduleName.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof ModuleConfiguration)) {
            return false;
        }
        ModuleConfiguration other = (ModuleConfiguration) obj;
        if (addExports == null) {
            if (other.addExports != null) {
                return false;
            }
        } else if (!addExports.equals(other.addExports)) {
            return false;
        }
        if (addOpens == null) {
            if (other.addOpens != null) {
                return false;
            }
        } else if (!addOpens.equals(other.addOpens)) {
            return false;
        }
        if (moduleName == null) {
            if (other.moduleName != null) {
                return false;
            }
        } else if (!moduleName.equals(other.moduleName)) {
            return false;
        }
        return true;
    }

    @Override
    public int compareTo(final ModuleConfiguration o) {
        return moduleName.compareTo(o.moduleName);
    }

}
