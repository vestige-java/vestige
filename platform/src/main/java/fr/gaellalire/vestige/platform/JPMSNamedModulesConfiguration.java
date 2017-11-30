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
 * @author Gael Lalire
 */
public class JPMSNamedModulesConfiguration implements Serializable {

    public static final JPMSNamedModulesConfiguration EMPTY_INSTANCE = new JPMSNamedModulesConfiguration(null, null, null);

    private static final long serialVersionUID = 610034627452670499L;

    private Set<AddReads> addReads;

    private Set<AddAccessibility> addExports;

    private Set<AddAccessibility> addOpens;

    public JPMSNamedModulesConfiguration(final Set<AddReads> addReads, final Set<AddAccessibility> addExports, final Set<AddAccessibility> addOpens) {
        this.addReads = addReads;
        this.addExports = addExports;
        this.addOpens = addOpens;
    }

    public Set<AddReads> getAddReads() {
        return addReads;
    }

    public Set<AddAccessibility> getAddExports() {
        return addExports;
    }

    public Set<AddAccessibility> getAddOpens() {
        return addOpens;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        if (addExports != null) {
            result = prime * result + addExports.hashCode();
        } else {
            result = prime * result;
        }
        if (addOpens != null) {
            result = prime * result + addOpens.hashCode();
        } else {
            result = prime * result;
        }
        if (addReads != null) {
            result = prime * result + addReads.hashCode();
        } else {
            result = prime * result;
        }
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof JPMSNamedModulesConfiguration)) {
            return false;
        }
        JPMSNamedModulesConfiguration other = (JPMSNamedModulesConfiguration) obj;
        if (addExports == null) {
            if (other.addExports != null && other.addExports.size() != 0) {
                return false;
            }
        } else if ((addExports.size() != 0 && other.addExports == null) || !addExports.equals(other.addExports)) {
            return false;
        }
        if (addOpens == null) {
            if (other.addOpens != null && other.addOpens.size() != 0) {
                return false;
            }
        } else if ((addOpens.size() != 0 && other.addOpens == null) || !addOpens.equals(other.addOpens)) {
            return false;
        }
        if (addReads == null) {
            if (other.addReads != null && other.addReads.size() != 0) {
                return false;
            }
        } else if ((addReads.size() != 0 && other.addReads == null) || !addReads.equals(other.addReads)) {
            return false;
        }
        return true;
    }

}
