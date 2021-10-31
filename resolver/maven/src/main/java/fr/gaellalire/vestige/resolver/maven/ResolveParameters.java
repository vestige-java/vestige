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

import java.util.List;

import org.eclipse.aether.impl.DependencyModifier;

/**
 * @author Gael Lalire
 */
public class ResolveParameters {

    private String groupId;

    private String artifactId;

    private String version;

    private String extension;

    private String classifier;

    private List<MavenRepository> additionalRepositories;

    private DependencyModifier dependencyModifier;

    private ArtifactPatcher artifactPatcher;

    private boolean superPomRepositoriesIgnored;

    private boolean pomRepositoriesIgnored;

    private boolean checksumVerified;

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(final String groupId) {
        this.groupId = groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public void setArtifactId(final String artifactId) {
        this.artifactId = artifactId;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(final String version) {
        this.version = version;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(final String extension) {
        this.extension = extension;
    }

    public List<MavenRepository> getAdditionalRepositories() {
        return additionalRepositories;
    }

    public void setAdditionalRepositories(final List<MavenRepository> additionalRepositories) {
        this.additionalRepositories = additionalRepositories;
    }

    public DependencyModifier getDependencyModifier() {
        return dependencyModifier;
    }

    public void setDependencyModifier(final DependencyModifier dependencyModifier) {
        this.dependencyModifier = dependencyModifier;
    }

    public boolean isSuperPomRepositoriesIgnored() {
        return superPomRepositoriesIgnored;
    }

    public void setSuperPomRepositoriesIgnored(final boolean superPomRepositoriesIgnored) {
        this.superPomRepositoriesIgnored = superPomRepositoriesIgnored;
    }

    public boolean isPomRepositoriesIgnored() {
        return pomRepositoriesIgnored;
    }

    public void setPomRepositoriesIgnored(final boolean pomRepositoriesIgnored) {
        this.pomRepositoriesIgnored = pomRepositoriesIgnored;
    }

    public boolean isChecksumVerified() {
        return checksumVerified;
    }

    public void setChecksumVerified(final boolean checksumVerified) {
        this.checksumVerified = checksumVerified;
    }

    public ArtifactPatcher getArtifactPatcher() {
        return artifactPatcher;
    }

    public void setArtifactPatcher(final ArtifactPatcher artifactPatcher) {
        this.artifactPatcher = artifactPatcher;
    }

    public void setClassifier(final String classifier) {
        this.classifier = classifier;
    }

    public String getClassifier() {
        return classifier;
    }

}
