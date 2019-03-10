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
public class MavenArtifact implements Serializable {

    private static final long serialVersionUID = 2208410848525695153L;

    private String groupId;

    private String artifactId;

    private String version;

    private String extension;

    // SHA1Sum is not in equals method, but it could be (not shared between two resolving)
    private String sha1sum;

    private int hashCode;

    public MavenArtifact(final String groupId, final String artifactId, final String version, final String extension, final String sha1sum) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.extension = extension;
        this.sha1sum = sha1sum;
        if (version == null) {
            hashCode = groupId.hashCode() + artifactId.hashCode();
        } else {
            hashCode = groupId.hashCode() + artifactId.hashCode() + version.hashCode();
        }
    }

    public String getGroupId() {
        return groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getVersion() {
        return version;
    }

    public String getExtension() {
        return extension;
    }

    public String getSha1sum() {
        return sha1sum;
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof MavenArtifact)) {
            return false;
        }
        MavenArtifact other = (MavenArtifact) obj;
        if (!artifactId.equals(other.artifactId)) {
            return false;
        }
        if (!groupId.equals(other.groupId)) {
            return false;
        }
        if (version == null) {
            if (other.version != null) {
                return false;
            }
        } else if (!version.equals(other.version)) {
            return false;
        }
        if (extension == null) {
            if (other.extension != null) {
                return false;
            }
        } else if (!extension.equals(other.extension)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        if (!"jar".equals(extension)) {
            return "mvn:" + groupId + "/" + artifactId + "/" + version + "/" + extension;
        }
        return "mvn:" + groupId + "/" + artifactId + "/" + version;
    }

}
