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
public class AddReads implements Serializable {

    private static final long serialVersionUID = 113069417607265579L;

    private String target;

    private String source;

    public AddReads(final String source, final String target) {
        this.source = source;
        this.target = target;
    }

    public String getTarget() {
        return target;
    }

    public String getSource() {
        return source;
    }

    @Override
    public int hashCode() {
        return source.hashCode() + target.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof AddReads)) {
            return false;
        }
        AddReads other = (AddReads) obj;
        if (!source.equals(other.source)) {
            return false;
        }
        if (!target.equals(other.target)) {
            return false;
        }
        return true;
    }

}
