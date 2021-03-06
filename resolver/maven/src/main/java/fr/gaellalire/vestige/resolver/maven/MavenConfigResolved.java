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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;

import fr.gaellalire.vestige.spi.job.JobHelper;
import fr.gaellalire.vestige.spi.resolver.ResolverException;
import fr.gaellalire.vestige.spi.resolver.maven.MavenContext;
import fr.gaellalire.vestige.spi.resolver.maven.MavenContextBuilder;
import fr.gaellalire.vestige.spi.resolver.maven.ModifyDependencyRequest;
import fr.gaellalire.vestige.spi.resolver.maven.ReplaceDependencyRequest;
import fr.gaellalire.vestige.spi.resolver.maven.ResolveMavenArtifactRequest;
import fr.gaellalire.vestige.spi.resolver.maven.ResolvedMavenArtifact;

/**
 * @author Gael Lalire
 */
public class MavenConfigResolved implements MavenContextBuilder, MavenContext {

    private MavenArtifactResolver mavenArtifactResolver;

    private boolean builded;

    private boolean superPomRepositoriesIgnored;

    private boolean pomRepositoriesIgnored;

    private List<MavenRepository> additionalRepositories;

    private DefaultDependencyModifier defaultDependencyModifier;

    private void checkBuild() {
        if (builded) {
            throw new IllegalStateException("MavenContext is already build");
        }
    }

    public MavenConfigResolved(final MavenArtifactResolver mavenArtifactResolver) {
        this.mavenArtifactResolver = mavenArtifactResolver;
        additionalRepositories = new ArrayList<MavenRepository>();
        defaultDependencyModifier = new DefaultDependencyModifier();
    }

    public boolean isSuperPomRepositoriesIgnored() {
        return superPomRepositoriesIgnored;
    }

    public boolean isPomRepositoriesIgnored() {
        return pomRepositoriesIgnored;
    }

    public List<MavenRepository> getAdditionalRepositories() {
        return additionalRepositories;
    }

    public DefaultDependencyModifier getDefaultDependencyModifier() {
        return defaultDependencyModifier;
    }

    @Override
    public void addAdditionalRepository(final String id, final String layout, final String url) {
        checkBuild();
        additionalRepositories.add(new MavenRepository(id, layout, url));
    }

    @Override
    public ModifyDependencyRequest addModifyDependency(final String groupId, final String artifactId) {
        return new ModifyDependencyRequest() {

            private List<Dependency> dependencies = new ArrayList<Dependency>();

            private List<MavenArtifactKey> removedDependencies = new ArrayList<MavenArtifactKey>();

            private Artifact patch;

            public void setPatch(final String groupId, final String artifactId, final String version) {
                patch = new DefaultArtifact(groupId, artifactId, "jar", version);
            }

            @Override
            public void addDependency(final String groupId, final String artifactId, final String version) {
                addDependency(groupId, artifactId, version, "jar");
            }

            @Override
            public void execute() {
                checkBuild();
                defaultDependencyModifier.setPatch(groupId, artifactId, patch);
                defaultDependencyModifier.add(groupId, artifactId, dependencies);
                defaultDependencyModifier.remove(groupId, artifactId, removedDependencies);
            }

            @Override
            public void removeDependency(final String groupId, final String artifactId, final String extension) {
                removedDependencies.add(new MavenArtifactKey(groupId, artifactId, extension));
            }

            @Override
            public void addDependency(final String groupId, final String artifactId, final String version, final String extension) {
                dependencies.add(new Dependency(new DefaultArtifact(groupId, artifactId, extension, version), "runtime"));
            }

        };
    }

    @Override
    public ReplaceDependencyRequest addReplaceDependency(final String groupId, final String artifactId) {
        return new ReplaceDependencyRequest() {

            private Map<String, Set<String>> exceptsMap;

            private List<Dependency> dependencies = new ArrayList<Dependency>();

            @Override
            public void addExcept(final String groupId, final String artifactId) {
                if (exceptsMap == null) {
                    exceptsMap = new HashMap<String, Set<String>>();
                }
                Set<String> set = exceptsMap.get(groupId);
                if (set == null) {
                    set = new HashSet<String>();
                    exceptsMap.put(groupId, set);
                }
                set.add(artifactId);
            }

            @Override
            public void addDependency(final String groupId, final String artifactId, final String version) {
                dependencies.add(new Dependency(new DefaultArtifact(groupId, artifactId, "jar", version), "runtime"));
            }

            @Override
            public void execute() {
                checkBuild();
                defaultDependencyModifier.replace(groupId, artifactId, dependencies, exceptsMap);
            }
        };
    }

    @Override
    public void setSuperPomRepositoriesIgnored(final boolean superPomRepositoriesIgnored) {
        checkBuild();
        this.superPomRepositoriesIgnored = superPomRepositoriesIgnored;
    }

    @Override
    public void setPomRepositoriesIgnored(final boolean pomRepositoriesIgnored) {
        checkBuild();
        this.pomRepositoriesIgnored = pomRepositoriesIgnored;
    }

    @Override
    public MavenContext build() {
        builded = true;
        return this;
    }

    @Override
    public ResolveMavenArtifactRequest resolve(final String groupId, final String artifactId, final String version) {
        final ResolveParameters resolveRequest = new ResolveParameters();
        resolveRequest.setGroupId(groupId);
        resolveRequest.setArtifactId(artifactId);
        resolveRequest.setVersion(version);
        return new ResolveMavenArtifactRequest() {

            private String extension = "jar";

            @Override
            public void setExtension(final String extension) {
                this.extension = extension;
            }

            @Override
            public ResolvedMavenArtifact execute(final JobHelper jobHelper) throws ResolverException {
                resolveRequest.setAdditionalRepositories(additionalRepositories);
                resolveRequest.setDependencyModifier(defaultDependencyModifier);
                resolveRequest.setSuperPomRepositoriesIgnored(superPomRepositoriesIgnored);
                resolveRequest.setPomRepositoriesIgnored(pomRepositoriesIgnored);
                resolveRequest.setExtension(extension);
                resolveRequest.setArtifactPatcher(defaultDependencyModifier);

                return mavenArtifactResolver.resolve(resolveRequest, jobHelper);
            }

        };

    }

}
