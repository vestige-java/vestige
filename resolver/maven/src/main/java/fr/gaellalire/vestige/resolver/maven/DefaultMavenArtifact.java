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

import fr.gaellalire.vestige.spi.resolver.maven.MavenArtifact;

/**
 * @author Gael Lalire
 */
public class DefaultMavenArtifact implements Serializable, MavenArtifact {

    private static final long serialVersionUID = 2208410848525695153L;

    private String groupId;

    private String artifactId;

    private String version;

    private String extension;

    private String classifier;

    private int hashCode;

    private boolean virtual;

    private boolean parentExcluder;

    private DefaultMavenArtifact patch;

    public DefaultMavenArtifact() {
        this(false);
    }

    public DefaultMavenArtifact(final boolean parentExcluder) {
        virtual = true;
        this.parentExcluder = parentExcluder;
    }

    public DefaultMavenArtifact(final String groupId, final String artifactId, final String version, final String extension, final String classifier,
            final DefaultMavenArtifact patch) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.extension = extension;
        this.classifier = classifier;
        this.patch = patch;
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

    public String getClassifier() {
        return classifier;
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    public boolean isVirtual() {
        return virtual;
    }

    public boolean isParentExcluder() {
        return parentExcluder;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof DefaultMavenArtifact)) {
            return false;
        }
        DefaultMavenArtifact other = (DefaultMavenArtifact) obj;
        if (virtual || other.virtual) {
            return false;
        }
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
        if (patch == null) {
            if (other.patch != null) {
                return false;
            }
        } else if (!patch.equals(other.patch)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        if (virtual) {
            return "virtual";
        }
        String append = "";
        if (patch != null) {
            append = " (" + patch.toString() + ")";
        }
        if (classifier != null && classifier.length() != 0) {
            return "mvn:" + groupId + "/" + artifactId + "/" + version + "/" + extension + "/" + classifier + append;
        }
        if (!"jar".equals(extension)) {
            return "mvn:" + groupId + "/" + artifactId + "/" + version + "/" + extension + append;
        }
        return "mvn:" + groupId + "/" + artifactId + "/" + version + append;
    }

}
