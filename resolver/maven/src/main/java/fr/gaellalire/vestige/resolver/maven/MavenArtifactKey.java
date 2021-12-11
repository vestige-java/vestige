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

import java.io.IOException;
import java.io.Serializable;

/**
 * @author Gael Lalire
 */
public class MavenArtifactKey implements Serializable {

    private static final long serialVersionUID = -8995236861812898191L;

    private String groupId;

    private String artifactId;

    private String extension;

    private String classifier;

    public MavenArtifactKey(final String groupId, final String artifactId, final String extension, final String classifier) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        if (extension == null || extension.length() == 0) {
            this.extension = "jar";
        } else {
            this.extension = extension;
        }
        if (classifier == null) {
            this.classifier = "";
        } else {
            this.classifier = classifier;
        }
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

    private void readObject(final java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        if (extension == null || extension.length() == 0) {
            extension = "jar";
        }
        if (classifier == null) {
            classifier = "";
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + groupId.hashCode();
        result = prime * result + artifactId.hashCode();
        result = prime * result + extension.hashCode();
        result = prime * result + classifier.hashCode();
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
        if (!groupId.equals(other.groupId)) {
            return false;
        }
        if (!artifactId.equals(other.artifactId)) {
            return false;
        }
        if (!extension.equals(other.extension)) {
            return false;
        }
        if (!classifier.equals(other.classifier)) {
            return false;
        }
        return true;
    }

}
