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

/**
 * @author Gael Lalire
 */
public class AddAccessibility implements Serializable {

    private static final long serialVersionUID = -8135765191097404634L;

    private String source;

    private String pn;

    private String target;

    public AddAccessibility(final String source, final String pn, final String target) {
        this.source = source;
        this.pn = pn;
        this.target = target;
    }

    public String getSource() {
        return source;
    }

    public String getPn() {
        return pn;
    }

    public String getTarget() {
        return target;
    }

    @Override
    public int hashCode() {
        return pn.hashCode() + source.hashCode() + target.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof AddAccessibility)) {
            return false;
        }
        AddAccessibility other = (AddAccessibility) obj;
        if (!pn.equals(other.pn)) {
            return false;
        }
        if (!source.equals(other.source)) {
            return false;
        }
        if (!target.equals(other.target)) {
            return false;
        }
        return true;
    }

}
