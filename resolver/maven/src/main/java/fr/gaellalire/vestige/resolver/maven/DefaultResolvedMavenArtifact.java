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

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.aether.artifact.Artifact;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.gaellalire.vestige.jpms.NamedModuleUtils;
import fr.gaellalire.vestige.platform.AddAccessibility;
import fr.gaellalire.vestige.platform.AddReads;
import fr.gaellalire.vestige.platform.ClassLoaderConfiguration;
import fr.gaellalire.vestige.platform.JPMSClassLoaderConfiguration;
import fr.gaellalire.vestige.platform.JPMSNamedModulesConfiguration;
import fr.gaellalire.vestige.platform.MinimalStringParserFactory;
import fr.gaellalire.vestige.platform.ModuleConfiguration;
import fr.gaellalire.vestige.platform.SecureFile;
import fr.gaellalire.vestige.platform.VestigePlatform;
import fr.gaellalire.vestige.resolver.common.DefaultResolvedClassLoaderConfiguration;
import fr.gaellalire.vestige.resolver.common.DefaultVestigeJar;
import fr.gaellalire.vestige.spi.resolver.ResolvedClassLoaderConfiguration;
import fr.gaellalire.vestige.spi.resolver.ResolverException;
import fr.gaellalire.vestige.spi.resolver.Scope;
import fr.gaellalire.vestige.spi.resolver.VestigeJar;
import fr.gaellalire.vestige.spi.resolver.maven.CreateClassLoaderConfigurationRequest;
import fr.gaellalire.vestige.spi.resolver.maven.ModifyLoadedDependencyRequest;
import fr.gaellalire.vestige.spi.resolver.maven.ResolveMode;
import fr.gaellalire.vestige.spi.resolver.maven.ResolvedMavenArtifact;

/**
 * @author Gael Lalire
 */
public class DefaultResolvedMavenArtifact implements ResolvedMavenArtifact {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultResolvedMavenArtifact.class);

    private List<MavenArtifactAndMetadata> artifacts;

    private Artifact artifact;

    private SecureFile secureFile;

    private DependencyReader dependencyReader;

    private NodeAndState nodeAndState;

    private boolean relevantDataLimited;

    private VestigePlatform vestigePlatform;

    private static final BeforeParentController NOOP_BEFORE_PARENT_CONTROLLER = new BeforeParentController() {

        @Override
        public boolean isBeforeParent(final String groupId, final String artifactId) {
            return false;
        }
    };

    public DefaultResolvedMavenArtifact(final VestigePlatform vestigePlatform, final DependencyReader dependencyReader, final NodeAndState nodeAndState,
            final List<MavenArtifactAndMetadata> artifacts, final boolean relevantDataLimited) {
        this.vestigePlatform = vestigePlatform;
        this.dependencyReader = dependencyReader;
        this.nodeAndState = nodeAndState;
        this.artifacts = artifacts;
        this.relevantDataLimited = relevantDataLimited;
        artifact = nodeAndState.getDependencyNode().getArtifact();
        if (relevantDataLimited) {
            secureFile = artifacts.get(0).getSecureFile();
        }
    }

    public String getGroupId() {
        return artifact.getGroupId();
    }

    public String getArtifactId() {
        return artifact.getGroupId();
    }

    public String getVersion() {
        return artifact.getVersion();
    }

    public String getExtension() {
        return artifact.getExtension();
    }

    public List<? extends DefaultResolvedMavenArtifact> getDependenciesAsList() throws ResolverException {
        List<NodeAndState> dependencies = dependencyReader.getDependencies(nodeAndState);
        List<DefaultResolvedMavenArtifact> resolvedMavenArtifacts = new ArrayList<DefaultResolvedMavenArtifact>(dependencies.size());
        for (NodeAndState nodeAndState : dependencies) {
            resolvedMavenArtifacts.add(new DefaultResolvedMavenArtifact(vestigePlatform, dependencyReader, nodeAndState, artifacts, false));
        }
        return resolvedMavenArtifacts;
    }

    public VestigeJar getVestigeJar() {
        if (secureFile == null) {
            for (MavenArtifactAndMetadata artifact : artifacts) {
                MavenArtifact mavenArtifact = artifact.getMavenArtifact();
                if (mavenArtifact.getArtifactId().equals(getArtifactId()) && mavenArtifact.getGroupId().equals(getGroupId())) {
                    secureFile = artifact.getSecureFile();
                    break;
                }
            }
        }
        return new DefaultVestigeJar(secureFile);
    }

    public ClassLoaderConfiguration createClassLoaderConfiguration(final CreateClassLoaderConfigurationParameters createClassLoaderConfigurationParameters)
            throws ResolverException {
        String appName = createClassLoaderConfigurationParameters.getAppName();

        JPMSNamedModulesConfiguration jpmsNamedModulesConfiguration = createClassLoaderConfigurationParameters.getJpmsNamedModulesConfiguration();
        Scope scope = createClassLoaderConfigurationParameters.getScope();
        BeforeParentController beforeParentController = createClassLoaderConfigurationParameters.getBeforeParentController();
        if (beforeParentController == null) {
            beforeParentController = NOOP_BEFORE_PARENT_CONTROLLER;
        }

        LOGGER.info("Creating classloader configuration for {}", appName);
        ClassLoaderConfiguration classLoaderConfiguration;

        DefaultJPMSConfiguration jpmsConfiguration = createClassLoaderConfigurationParameters.getJpmsConfiguration();
        if (jpmsConfiguration == null) {
            jpmsConfiguration = new DefaultJPMSConfiguration();
        }

        if (createClassLoaderConfigurationParameters.isManyLoaders()) {
            Map<String, Map<String, MavenArtifact>> runtimeDependencies = new HashMap<String, Map<String, MavenArtifact>>();
            Map<MavenArtifact, File> urlByKey = new HashMap<MavenArtifact, File>();
            for (MavenArtifactAndMetadata artifact : artifacts) {
                MavenArtifact mavenArtifact = artifact.getMavenArtifact();
                Map<String, MavenArtifact> map = runtimeDependencies.get(mavenArtifact.getGroupId());
                if (map == null) {
                    map = new HashMap<String, MavenArtifact>();
                    runtimeDependencies.put(mavenArtifact.getGroupId(), map);
                }
                map.put(mavenArtifact.getArtifactId(), mavenArtifact);
                urlByKey.put(mavenArtifact, artifact.getSecureFile().getFile());
            }

            if (createClassLoaderConfigurationParameters.isSelfExcluded()) {
                runtimeDependencies.get(getGroupId()).put(getArtifactId(), new MavenArtifact());
            }

            ClassLoaderConfigurationGraphHelper classLoaderConfigurationGraphHelper = new ClassLoaderConfigurationGraphHelper(appName, urlByKey, dependencyReader,
                    beforeParentController, jpmsConfiguration, runtimeDependencies, scope, createClassLoaderConfigurationParameters.getScopeModifier(),
                    jpmsNamedModulesConfiguration);

            GraphCycleRemover<NodeAndState, MavenArtifact, ClassLoaderConfigurationFactory> graphCycleRemover = new GraphCycleRemover<NodeAndState, MavenArtifact, ClassLoaderConfigurationFactory>(
                    classLoaderConfigurationGraphHelper);
            ClassLoaderConfigurationFactory removeCycle = graphCycleRemover.removeCycle(nodeAndState);
            if (jpmsNamedModulesConfiguration != null) {
                removeCycle.setNamedModulesConfiguration(jpmsNamedModulesConfiguration);
            }
            classLoaderConfiguration = removeCycle.create(new MinimalStringParserFactory());
        } else {
            List<SecureFile> beforeUrls = new ArrayList<SecureFile>();
            List<SecureFile> afterUrls = new ArrayList<SecureFile>();
            List<MavenArtifact> mavenArtifacts = new ArrayList<MavenArtifact>();
            JPMSClassLoaderConfiguration moduleConfiguration = JPMSClassLoaderConfiguration.EMPTY_INSTANCE;

            if (!relevantDataLimited) {
                artifacts = dependencyReader.retainDependencies(nodeAndState, artifacts);
                relevantDataLimited = true;
            }

            List<MavenArtifactAndMetadata> artifacts;
            if (createClassLoaderConfigurationParameters.isSelfExcluded()) {
                artifacts = this.artifacts.subList(1, this.artifacts.size());
            } else {
                artifacts = this.artifacts;
            }
            boolean[] beforeParents = null;
            int i = 0;
            for (MavenArtifactAndMetadata artifact : artifacts) {
                MavenArtifact mavenArtifact = artifact.getMavenArtifact();
                mavenArtifacts.add(mavenArtifact);
                List<SecureFile> urls;
                if (beforeParentController.isBeforeParent(mavenArtifact.getGroupId(), mavenArtifact.getArtifactId())) {
                    urls = beforeUrls;
                    if (beforeParents == null) {
                        beforeParents = new boolean[artifacts.size()];
                    }
                    beforeParents[i] = true;
                } else {
                    urls = afterUrls;
                }
                File file = artifact.getSecureFile().getFile();
                try {
                    urls.add(new SecureFile(file, new URL(mavenArtifact.toString()), mavenArtifact.getSha1sum()));
                } catch (MalformedURLException e) {
                    throw new ResolverException("Unable to create Maven URL", e);
                }

                JPMSClassLoaderConfiguration unnamedClassLoaderConfiguration = jpmsConfiguration.getModuleConfiguration(mavenArtifact.getGroupId(), mavenArtifact.getArtifactId());
                if (jpmsNamedModulesConfiguration != null) {
                    String moduleName;
                    try {
                        moduleName = NamedModuleUtils.getModuleName(file);
                    } catch (IOException e) {
                        throw new ResolverException("Unable to calculate module name", e);
                    }

                    List<ModuleConfiguration> namedModuleConfigurations = new ArrayList<ModuleConfiguration>();
                    for (ModuleConfiguration unnamedModuleConfiguration : unnamedClassLoaderConfiguration.getModuleConfigurations()) {
                        namedModuleConfigurations.add(new ModuleConfiguration(unnamedModuleConfiguration.getModuleName(), unnamedModuleConfiguration.getAddExports(),
                                unnamedModuleConfiguration.getAddOpens(), moduleName));
                    }
                    moduleConfiguration = moduleConfiguration.merge(namedModuleConfigurations);
                } else {
                    moduleConfiguration = moduleConfiguration.merge(unnamedClassLoaderConfiguration);
                }
                i++;
            }
            MavenClassLoaderConfigurationKey key = new MavenClassLoaderConfigurationKey(mavenArtifacts, Collections.<MavenClassLoaderConfigurationKey> emptyList(), scope,
                    moduleConfiguration, jpmsNamedModulesConfiguration, beforeParents);
            String name;
            if (scope == Scope.PLATFORM) {
                name = key.getArtifacts().toString();
            } else {
                name = appName;
            }
            classLoaderConfiguration = new ClassLoaderConfiguration(key, name, scope == Scope.ATTACHMENT, beforeUrls, afterUrls, Collections.<ClassLoaderConfiguration> emptyList(),
                    null, null, null, null, key.getModuleConfiguration(), jpmsNamedModulesConfiguration);
        }
        LOGGER.info("Classloader configuration created");
        return classLoaderConfiguration;
    }

    @Override
    public Enumeration<? extends ResolvedMavenArtifact> getDependencies() throws ResolverException {
        return Collections.enumeration(getDependenciesAsList());
    }

    @Override
    public CreateClassLoaderConfigurationRequest createClassLoaderConfiguration(final String name, final ResolveMode mode, final Scope scope) {
        final CreateClassLoaderConfigurationParameters resolveRequest = new CreateClassLoaderConfigurationParameters();

        resolveRequest.setScope(scope);
        resolveRequest.setAppName(name);
        resolveRequest.setManyLoaders(mode == ResolveMode.FIXED_DEPENDENCIES);

        return new CreateClassLoaderConfigurationRequest() {

            private boolean namedModuleActivated;

            private Set<AddReads> addReads;

            private Set<AddAccessibility> addExports;

            private Set<AddAccessibility> addOpens;

            private ScopeModifier scopeModifier;

            private boolean selfExcluded;

            private DefaultJPMSConfiguration defaultJPMSConfiguration = new DefaultJPMSConfiguration();

            private DefaultDependencyModifier beforeParentController = new DefaultDependencyModifier();

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

            public void addModifyScope(final String groupId, final String artifactId, final Scope scope) {
                if (scopeModifier == null) {
                    scopeModifier = new ScopeModifier();
                }
                scopeModifier.put(groupId, artifactId, scope);
            }

            @Override
            public void setSelfExcluded(final boolean selfExcluded) {
                this.selfExcluded = selfExcluded;
            }

            @Override
            public ResolvedClassLoaderConfiguration execute() throws ResolverException {
                JPMSNamedModulesConfiguration jpmsNamedModulesConfiguration = null;
                if (namedModuleActivated) {
                    if (addReads == null && addExports == null && addOpens == null) {
                        jpmsNamedModulesConfiguration = JPMSNamedModulesConfiguration.EMPTY_INSTANCE;
                    } else {
                        jpmsNamedModulesConfiguration = new JPMSNamedModulesConfiguration(addReads, addExports, addOpens);
                    }
                }

                resolveRequest.setJpmsConfiguration(defaultJPMSConfiguration);
                resolveRequest.setJpmsNamedModulesConfiguration(jpmsNamedModulesConfiguration);
                resolveRequest.setScopeModifier(scopeModifier);
                resolveRequest.setSelfExcluded(selfExcluded);

                return new DefaultResolvedClassLoaderConfiguration(vestigePlatform, createClassLoaderConfiguration(resolveRequest),
                        beforeParentController.isBeforeParent(getGroupId(), getArtifactId()));

            }

            @Override
            public ModifyLoadedDependencyRequest addModifyLoadedDependency(final String groupId, final String artifactId) {

                return new ModifyLoadedDependencyRequest() {

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
                    public void setBeforeParent(final boolean beforeParent) {
                        this.beforeParent = beforeParent;
                    }

                    @Override
                    public void execute() {
                        defaultJPMSConfiguration.addModuleConfiguration(groupId, artifactId, moduleConfigurations);
                        if (beforeParent) {
                            beforeParentController.addBeforeParent(groupId, artifactId);
                        }
                    }

                };
            }
        };
    }

}
