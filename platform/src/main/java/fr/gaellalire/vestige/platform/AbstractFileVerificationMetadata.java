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

/**
 * @author Gael Lalire
 */
public abstract class AbstractFileVerificationMetadata {

    private long size;

    private String sha512;

    public AbstractFileVerificationMetadata(final long size, final String sha512) {
        this.size = size;
        this.sha512 = sha512;
    }

    public long getSize() {
        return size;
    }

    public String getSha512() {
        return sha512;
    }

    @Override
    public String toString() {
        return size + "/" + sha512;
    }

}
