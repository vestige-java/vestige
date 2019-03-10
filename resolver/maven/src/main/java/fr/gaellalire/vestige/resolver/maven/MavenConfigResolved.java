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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;

import fr.gaellalire.vestige.platform.AddAccessibility;
import fr.gaellalire.vestige.platform.AddReads;
import fr.gaellalire.vestige.platform.ClassLoaderConfiguration;
import fr.gaellalire.vestige.platform.JPMSNamedModulesConfiguration;
import fr.gaellalire.vestige.platform.ModuleConfiguration;
import fr.gaellalire.vestige.platform.VestigePlatform;
import fr.gaellalire.vestige.resolver.common.DefaultResolvedClassLoaderConfiguration;
import fr.gaellalire.vestige.spi.job.JobHelper;
import fr.gaellalire.vestige.spi.resolver.ResolvedClassLoaderConfiguration;
import fr.gaellalire.vestige.spi.resolver.ResolverException;
import fr.gaellalire.vestige.spi.resolver.Scope;
import fr.gaellalire.vestige.spi.resolver.maven.MavenContext;
import fr.gaellalire.vestige.spi.resolver.maven.MavenContextBuilder;
import fr.gaellalire.vestige.spi.resolver.maven.ModifyDependencyRequest;
import fr.gaellalire.vestige.spi.resolver.maven.ReplaceDependencyRequest;
import fr.gaellalire.vestige.spi.resolver.maven.ResolveMavenArtifactRequest;
import fr.gaellalire.vestige.spi.resolver.maven.ResolveMode;

/**
 * @author Gael Lalire
 */
public class MavenConfigResolved implements MavenContextBuilder, MavenContext {

    private VestigePlatform vestigePlatform;

    private MavenArtifactResolver mavenArtifactResolver;

    private boolean builded;

    private boolean superPomRepositoriesUsed;

    private boolean pomRepositoriesIgnored;

    private List<MavenRepository> additionalRepositories;

    private DefaultDependencyModifier defaultDependencyModifier;

    private DefaultJPMSConfiguration defaultJPMSConfiguration;

    private void checkBuild() {
        if (builded) {
            throw new IllegalStateException("MavenContext is already build");
        }
    }

    public MavenConfigResolved(final MavenArtifactResolver mavenArtifactResolver, final VestigePlatform vestigePlatform) {
        this.mavenArtifactResolver = mavenArtifactResolver;
        this.vestigePlatform = vestigePlatform;
        superPomRepositoriesUsed = true;
        pomRepositoriesIgnored = false;
        additionalRepositories = new ArrayList<MavenRepository>();
        defaultDependencyModifier = new DefaultDependencyModifier();
        defaultJPMSConfiguration = new DefaultJPMSConfiguration();
    }

    public boolean isSuperPomRepositoriesUsed() {
        return superPomRepositoriesUsed;
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

    public DefaultJPMSConfiguration getDefaultJPMSConfiguration() {
        return defaultJPMSConfiguration;
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

            private List<ModuleConfiguration> moduleConfigurations = new ArrayList<ModuleConfiguration>();

            private boolean beforeParent = false;

            @Override
            public void addOpens(final String moduleName, final String packageName) {
                moduleConfigurations.add(new ModuleConfiguration(moduleName, Collections.<String> emptySet(), Collections.singleton(packageName), null));
            }

            @Override
            public void addExports(final String moduleName, final String packageName) {
                moduleConfigurations.add(new ModuleConfiguration(moduleName, Collections.singleton(packageName), Collections.<String> emptySet(), null));
            }

            @Override
            public void addDependency(final String groupId, final String artifactId, final String version) {
                dependencies.add(new Dependency(new DefaultArtifact(groupId, artifactId, "jar", version), "runtime"));
            }

            @Override
            public void setBeforeParent(final boolean beforeParent) {
                this.beforeParent = beforeParent;
            }

            @Override
            public void execute() {
                checkBuild();
                defaultJPMSConfiguration.addModuleConfiguration(groupId, artifactId, moduleConfigurations);
                if (beforeParent) {
                    defaultDependencyModifier.addBeforeParent(groupId, artifactId);
                }
                defaultDependencyModifier.add(groupId, artifactId, dependencies);
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
    public void setSuperPomRepositoriesUsed(final boolean superPomRepositoriesUsed) {
        checkBuild();
        this.superPomRepositoriesUsed = superPomRepositoriesUsed;
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
    public ResolveMavenArtifactRequest resolve(final ResolveMode resolveMode, final Scope scope, final String groupId, final String artifactId, final String version,
            final String extension, final String name) {
        return new ResolveMavenArtifactRequest() {

            private boolean namedModuleActivated;

            private Set<AddReads> addReads;

            private Set<AddAccessibility> addExports;

            private Set<AddAccessibility> addOpens;

            private ScopeModifier scopeModifier;

            public void addModifyScope(final String groupId, final String artifactId, final Scope scope) {
                if (scopeModifier == null) {
                    scopeModifier = new ScopeModifier();
                }
                scopeModifier.put(groupId, artifactId, scope);
            }

            @Override
            public ResolvedClassLoaderConfiguration execute(final JobHelper jobHelper) throws ResolverException {
                JPMSNamedModulesConfiguration jpmsNamedModulesConfiguration = null;
                if (namedModuleActivated) {
                    if (addReads == null && addExports == null && addOpens == null) {
                        jpmsNamedModulesConfiguration = JPMSNamedModulesConfiguration.EMPTY_INSTANCE;
                    } else {
                        jpmsNamedModulesConfiguration = new JPMSNamedModulesConfiguration(addReads, addExports, addOpens);
                    }
                }

                final ClassLoaderConfiguration classLoaderConfiguration = mavenArtifactResolver.resolve(name, groupId, artifactId, version, extension, additionalRepositories,
                        defaultDependencyModifier, defaultJPMSConfiguration, jpmsNamedModulesConfiguration, resolveMode == ResolveMode.FIXED_DEPENDENCIES, scope, scopeModifier,
                        superPomRepositoriesUsed, pomRepositoriesIgnored, false, jobHelper);
                return new DefaultResolvedClassLoaderConfiguration(vestigePlatform, classLoaderConfiguration, defaultDependencyModifier.isBeforeParent(groupId, artifactId));
            }

            @Override
            public void setNamedModuleActivated(final boolean namedModuleActivated) {
                this.namedModuleActivated = namedModuleActivated;
            }

            @Override
            public void addReads(final String source, final String target) {
                if (addReads == null) {
                    addReads = new HashSet<AddReads>();
                }
                addReads.add(new AddReads(source, target));
            }

            @Override
            public void addExports(final String source, final String pn, final String target) {
                if (addExports == null) {
                    addExports = new HashSet<AddAccessibility>();
                }
                addExports.add(new AddAccessibility(source, pn, target));
            }

            @Override
            public void addOpens(final String source, final String pn, final String target) {
                if (addOpens == null) {
                    addOpens = new HashSet<AddAccessibility>();
                }
                addOpens.add(new AddAccessibility(source, pn, target));
            }
        };

    }

    @Override
    public ResolveMavenArtifactRequest resolve(final ResolveMode resolveMode, final Scope scope, final String groupId, final String artifactId, final String version,
            final String name) {
        return resolve(resolveMode, scope, groupId, artifactId, version, "jar", name);
    }

}
