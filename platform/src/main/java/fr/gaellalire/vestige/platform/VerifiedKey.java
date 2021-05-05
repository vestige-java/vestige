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
public class VerifiedKey implements Serializable {

    private static final long serialVersionUID = 4833511246412252528L;

    private Serializable key;

    private String verificationMetadata;

    public VerifiedKey(final Serializable key, final String verificationMetadata) {
        this.key = key;
        this.verificationMetadata = verificationMetadata;
    }

    @Override
    public String toString() {
        return key.toString();
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof VerifiedKey)) {
            return false;
        }
        VerifiedKey other = (VerifiedKey) obj;
        if (!key.equals(other.key)) {
            return false;
        }
        if (!verificationMetadata.equals(other.verificationMetadata)) {
            return false;
        }
        return true;
    }

}
