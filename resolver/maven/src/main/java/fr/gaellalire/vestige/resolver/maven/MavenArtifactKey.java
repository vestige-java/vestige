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

import java.io.Serializable;

/**
 * @author Gael Lalire
 */
public class MavenArtifactKey implements Serializable {

    private static final long serialVersionUID = -8995236861812898191L;

    private String groupId;

    private String artifactId;

    private String extension;

    public MavenArtifactKey(final String groupId, final String artifactId, final String extension) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.extension = extension;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getExtension() {
        return extension;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        if (artifactId == null) {
            result = prime * result;
        } else {
            result = prime * result + artifactId.hashCode();
        }
        if (extension == null) {
            result = prime * result;
        } else {
            result = prime * result + extension.hashCode();
        }
        if (groupId == null) {
            result = prime * result;
        } else {
            result = prime * result + groupId.hashCode();
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
        if (!(obj instanceof MavenArtifactKey)) {
            return false;
        }
        MavenArtifactKey other = (MavenArtifactKey) obj;
        if (artifactId == null) {
            if (other.artifactId != null) {
                return false;
            }
        } else if (!artifactId.equals(other.artifactId)) {
            return false;
        }
        if (extension == null) {
            if (other.extension != null) {
                return false;
            }
        } else if (!extension.equals(other.extension)) {
            return false;
        }
        if (groupId == null) {
            if (other.groupId != null) {
                return false;
            }
        } else if (!groupId.equals(other.groupId)) {
            return false;
        }
        return true;
    }

}
