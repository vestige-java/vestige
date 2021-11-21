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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.aether.artifact.Artifact;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.gaellalire.vestige.core.executor.VestigeWorker;
import fr.gaellalire.vestige.jpms.NamedModuleUtils;
import fr.gaellalire.vestige.platform.AddAccessibility;
import fr.gaellalire.vestige.platform.AddReads;
import fr.gaellalire.vestige.platform.ClassLoaderConfiguration;
import fr.gaellalire.vestige.platform.FileWithMetadata;
import fr.gaellalire.vestige.platform.JPMSClassLoaderConfiguration;
import fr.gaellalire.vestige.platform.JPMSNamedModulesConfiguration;
import fr.gaellalire.vestige.platform.MinimalStringParserFactory;
import fr.gaellalire.vestige.platform.ModuleConfiguration;
import fr.gaellalire.vestige.platform.VestigePlatform;
import fr.gaellalire.vestige.spi.job.DummyJobHelper;
import fr.gaellalire.vestige.spi.job.JobHelper;
import fr.gaellalire.vestige.spi.job.TaskHelper;
import fr.gaellalire.vestige.spi.resolver.ResolverException;
import fr.gaellalire.vestige.spi.resolver.Scope;
import fr.gaellalire.vestige.spi.resolver.maven.CreateClassLoaderConfigurationRequest;
import fr.gaellalire.vestige.spi.resolver.maven.MavenResolvedClassLoaderConfiguration;
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

    private FileWithMetadata fileWithMetadata;

    private DependencyReader dependencyReader;

    private NodeAndState nodeAndState;

    private boolean relevantDataLimited;

    private VestigePlatform vestigePlatform;

    private VestigeWorker vestigeWorker;

    private static final BeforeParentController NOOP_BEFORE_PARENT_CONTROLLER = new BeforeParentController() {

        @Override
        public boolean isBeforeParent(final String groupId, final String artifactId) {
            return false;
        }
    };

    public DefaultResolvedMavenArtifact(final VestigePlatform vestigePlatform, final VestigeWorker vestigeWorker, final DependencyReader dependencyReader,
            final NodeAndState nodeAndState, final List<MavenArtifactAndMetadata> artifacts, final boolean relevantDataLimited) {
        this.vestigePlatform = vestigePlatform;
        this.vestigeWorker = vestigeWorker;
        this.dependencyReader = dependencyReader;
        this.nodeAndState = nodeAndState;
        this.artifacts = artifacts;
        this.relevantDataLimited = relevantDataLimited;
        artifact = nodeAndState.getDependencyNode().getArtifact();
        if (relevantDataLimited) {
            fileWithMetadata = artifacts.get(0).getFileWithMetadata();
        }
    }

    public String getGroupId() {
        return artifact.getGroupId();
    }

    public String getArtifactId() {
        return artifact.getArtifactId();
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
            resolvedMavenArtifacts.add(new DefaultResolvedMavenArtifact(vestigePlatform, vestigeWorker, dependencyReader, nodeAndState, artifacts, false));
        }
        return resolvedMavenArtifacts;
    }

    @Override
    public File getFile() {
        if (fileWithMetadata == null) {
            for (MavenArtifactAndMetadata artifact : artifacts) {
                DefaultMavenArtifact mavenArtifact = artifact.getMavenArtifact();
                if (mavenArtifact.getArtifactId().equals(getArtifactId()) && mavenArtifact.getGroupId().equals(getGroupId())) {
                    fileWithMetadata = artifact.getFileWithMetadata();
                    break;
                }
            }
        }
        return fileWithMetadata.getFile();
    }

    public ClassLoaderConfiguration createClassLoaderConfiguration(final CreateClassLoaderConfigurationParameters createClassLoaderConfigurationParameters,
            final List<DefaultMavenArtifact> mavenArtifacts, final JobHelper jobHelper) throws ResolverException {
        String appName = createClassLoaderConfigurationParameters.getAppName();

        TaskHelper taskHelper = jobHelper.addTask("Creating classloader configuration for " + appName);
        taskHelper.setProgress(0);

        List<DefaultMavenArtifact> notNullMavenArtifacts = mavenArtifacts;
        if (notNullMavenArtifacts == null) {
            notNullMavenArtifacts = new ArrayList<DefaultMavenArtifact>();
        }

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

        Set<MavenArtifactKey> excludesWithParents = createClassLoaderConfigurationParameters.getExcludesWithParents();
        Set<MavenArtifactKey> excludes = createClassLoaderConfigurationParameters.getExcludes();
        if (createClassLoaderConfigurationParameters.isManyLoaders() && !createClassLoaderConfigurationParameters.isDependenciesExcluded()) {
            Map<String, Map<String, DefaultMavenArtifact>> runtimeDependencies = new HashMap<String, Map<String, DefaultMavenArtifact>>();
            Map<DefaultMavenArtifact, FileWithMetadata> urlByKey = new HashMap<DefaultMavenArtifact, FileWithMetadata>();
            for (MavenArtifactAndMetadata artifact : artifacts) {
                DefaultMavenArtifact mavenArtifact = artifact.getMavenArtifact();
                Map<String, DefaultMavenArtifact> map = runtimeDependencies.get(mavenArtifact.getGroupId());
                if (map == null) {
                    map = new HashMap<String, DefaultMavenArtifact>();
                    runtimeDependencies.put(mavenArtifact.getGroupId(), map);
                }
                map.put(mavenArtifact.getArtifactId(), mavenArtifact);
                urlByKey.put(mavenArtifact, artifact.getFileWithMetadata());
            }

            if (createClassLoaderConfigurationParameters.isSelfExcluded()) {
                runtimeDependencies.get(getGroupId()).put(getArtifactId(), new DefaultMavenArtifact());
            }
            if (excludesWithParents != null || excludes != null) {
                if (excludes != null && excludes.size() != 0) {
                    for (MavenArtifactKey exclude : excludes) {
                        Map<String, DefaultMavenArtifact> map = runtimeDependencies.get(exclude.getGroupId());
                        if (map != null) {
                            map.put(exclude.getArtifactId(), new DefaultMavenArtifact());
                        }
                    }
                }
                if (excludesWithParents != null && excludesWithParents.size() != 0) {
                    for (MavenArtifactKey exclude : excludesWithParents) {
                        Map<String, DefaultMavenArtifact> map = runtimeDependencies.get(exclude.getGroupId());
                        if (map != null) {
                            map.put(exclude.getArtifactId(), new DefaultMavenArtifact(true));
                        }
                    }
                }

            }

            ClassLoaderConfigurationGraphHelper classLoaderConfigurationGraphHelper = new ClassLoaderConfigurationGraphHelper(appName, urlByKey, dependencyReader,
                    beforeParentController, jpmsConfiguration, runtimeDependencies, scope, createClassLoaderConfigurationParameters.getScopeModifier(),
                    jpmsNamedModulesConfiguration, notNullMavenArtifacts, taskHelper, .9f);

            GraphCycleRemover<NodeAndState, DefaultMavenArtifact, ClassLoaderConfigurationFactory> graphCycleRemover = new GraphCycleRemover<NodeAndState, DefaultMavenArtifact, ClassLoaderConfigurationFactory>(
                    classLoaderConfigurationGraphHelper);
            ClassLoaderConfigurationFactory removeCycle = graphCycleRemover.removeCycle(nodeAndState);
            if (jpmsNamedModulesConfiguration != null) {
                removeCycle.setNamedModulesConfiguration(jpmsNamedModulesConfiguration);
            }

            // process exclusion for partialAttach

            classLoaderConfiguration = removeCycle.create(new MinimalStringParserFactory(), taskHelper, removeCycle.getRecursiveFactoryDependencies().size(), .9f, .1f,
                    new int[] {0});
        } else {
            List<FileWithMetadata> beforeUrls = new ArrayList<FileWithMetadata>();
            List<FileWithMetadata> afterUrls = new ArrayList<FileWithMetadata>();
            JPMSClassLoaderConfiguration moduleConfiguration = JPMSClassLoaderConfiguration.EMPTY_INSTANCE;

            if (!relevantDataLimited) {
                artifacts = dependencyReader.retainDependencies(nodeAndState, artifacts);
                relevantDataLimited = true;
            }

            List<MavenArtifactAndMetadata> artifacts = new ArrayList<MavenArtifactAndMetadata>();

            if (createClassLoaderConfigurationParameters.isSelfExcluded()) {
                if (!createClassLoaderConfigurationParameters.isDependenciesExcluded()) {
                    artifacts.addAll(this.artifacts.subList(1, this.artifacts.size()));
                }
            } else if (createClassLoaderConfigurationParameters.isDependenciesExcluded()) {
                artifacts.addAll(this.artifacts.subList(0, 1));
            } else {
                artifacts.addAll(this.artifacts);
            }

            if (excludesWithParents != null || excludes != null) {

                Map<String, Map<String, DefaultMavenArtifact>> runtimeDependencies = new HashMap<String, Map<String, DefaultMavenArtifact>>();

                for (MavenArtifactAndMetadata artifact : this.artifacts) {
                    DefaultMavenArtifact mavenArtifact = artifact.getMavenArtifact();
                    Map<String, DefaultMavenArtifact> map = runtimeDependencies.get(mavenArtifact.getGroupId());
                    if (map == null) {
                        map = new HashMap<String, DefaultMavenArtifact>();
                        runtimeDependencies.put(mavenArtifact.getGroupId(), map);
                    }
                    map.put(mavenArtifact.getArtifactId(), mavenArtifact);
                }

                if (excludes != null && excludes.size() != 0) {
                    for (MavenArtifactKey exclude : excludes) {
                        Map<String, DefaultMavenArtifact> map = runtimeDependencies.get(exclude.getGroupId());
                        if (map != null) {
                            map.put(exclude.getArtifactId(), new DefaultMavenArtifact());
                        }
                    }
                }

                if (excludesWithParents != null && excludesWithParents.size() != 0) {
                    for (MavenArtifactKey exclude : excludesWithParents) {
                        Map<String, DefaultMavenArtifact> map = runtimeDependencies.get(exclude.getGroupId());
                        if (map != null) {
                            map.put(exclude.getArtifactId(), new DefaultMavenArtifact(true));
                        }
                    }
                }

                if (createClassLoaderConfigurationParameters.isSelfExcluded()) {
                    runtimeDependencies.get(getGroupId()).put(getArtifactId(), new DefaultMavenArtifact());
                }

                GraphCycleRemover<NodeAndState, DefaultMavenArtifact, Set<MavenArtifactKey>> graphCycleRemover = new GraphCycleRemover<NodeAndState, DefaultMavenArtifact, Set<MavenArtifactKey>>(
                        new ParentExcluderGraphHelper(dependencyReader, runtimeDependencies));
                Set<MavenArtifactKey> includedMavenArtifactKeys = graphCycleRemover.removeCycle(nodeAndState);

                Iterator<MavenArtifactAndMetadata> iterator = artifacts.iterator();
                while (iterator.hasNext()) {
                    MavenArtifactAndMetadata mavenArtifactAndMetadata = iterator.next();
                    DefaultMavenArtifact mavenArtifact = mavenArtifactAndMetadata.getMavenArtifact();
                    if (!includedMavenArtifactKeys.contains(
                            new MavenArtifactKey(mavenArtifact.getGroupId(), mavenArtifact.getArtifactId(), mavenArtifact.getExtension(), mavenArtifact.getClassifier()))) {
                        iterator.remove();
                    }
                }

            }

            boolean mdcIncluded = false;
            boolean[] beforeParents = null;
            int i = 0;
            int size = artifacts.size();
            for (MavenArtifactAndMetadata artifact : artifacts) {
                DefaultMavenArtifact mavenArtifact = artifact.getMavenArtifact();
                if ("pom".equals(mavenArtifact.getExtension())) {
                    i++;
                    taskHelper.setProgress(((float) i) / size);
                    continue;
                }
                if ("org.slf4j".equals(mavenArtifact.getGroupId()) && "slf4j-api".equals(mavenArtifact.getArtifactId())) {
                    mdcIncluded = true;
                }
                notNullMavenArtifacts.add(mavenArtifact);
                List<FileWithMetadata> urls;
                if (beforeParentController.isBeforeParent(mavenArtifact.getGroupId(), mavenArtifact.getArtifactId())) {
                    urls = beforeUrls;
                    if (beforeParents == null) {
                        beforeParents = new boolean[artifacts.size()];
                    }
                    beforeParents[i] = true;
                } else {
                    urls = afterUrls;
                }
                urls.add(artifact.getFileWithMetadata());

                JPMSClassLoaderConfiguration unnamedClassLoaderConfiguration = jpmsConfiguration.getModuleConfiguration(
                        new MavenArtifactKey(mavenArtifact.getGroupId(), mavenArtifact.getArtifactId(), mavenArtifact.getExtension(), mavenArtifact.getClassifier()));
                if (jpmsNamedModulesConfiguration != null) {
                    String moduleName;
                    try {
                        moduleName = NamedModuleUtils.getModuleName(artifact.getFileWithMetadata().getFile());
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
                taskHelper.setProgress(((float) i) / size);
            }
            MavenClassLoaderConfigurationKey key = new MavenClassLoaderConfigurationKey(notNullMavenArtifacts, Collections.<MavenClassLoaderConfigurationKey> emptyList(), scope,
                    moduleConfiguration, jpmsNamedModulesConfiguration, beforeParents);
            String name;
            if (scope == Scope.PLATFORM) {
                name = key.getArtifacts().toString();
            } else {
                name = appName;
            }
            classLoaderConfiguration = new ClassLoaderConfiguration(key, name, scope == Scope.ATTACHMENT, beforeUrls, afterUrls, Collections.<ClassLoaderConfiguration> emptyList(),
                    null, null, null, null, key.getModuleConfiguration(), jpmsNamedModulesConfiguration, mdcIncluded);
        }
        taskHelper.setDone();
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

            private Set<MavenArtifactKey> excludesWithParents;

            private Set<MavenArtifactKey> excludes;

            private ScopeModifier scopeModifier;

            private boolean selfExcluded;

            private boolean dependenciesExcluded;

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
            public MavenResolvedClassLoaderConfiguration execute() throws ResolverException {
                return execute(DummyJobHelper.INSTANCE);
            }

            public MavenResolvedClassLoaderConfiguration execute(final JobHelper jobHelper) throws ResolverException {
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
                resolveRequest.setDependenciesExcluded(dependenciesExcluded);
                resolveRequest.setExcludesWithParents(excludesWithParents);
                resolveRequest.setExcludes(excludes);

                List<DefaultMavenArtifact> mavenArtifacts = new ArrayList<DefaultMavenArtifact>();

                return new DefaultMavenResolvedClassLoaderConfiguration(vestigePlatform, vestigeWorker, createClassLoaderConfiguration(resolveRequest, mavenArtifacts, jobHelper),
                        beforeParentController.isBeforeParent(getGroupId(), getArtifactId()), mavenArtifacts);
            }

            @Override
            public ModifyLoadedDependencyRequest addModifyLoadedDependency(final String groupId, final String artifactId) {
                return addModifyLoadedDependency(groupId, artifactId, "");
            }

            @Override
            public ModifyLoadedDependencyRequest addModifyLoadedDependency(final String groupId, final String artifactId, final String classifier) {

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
                        defaultJPMSConfiguration.addModuleConfiguration(new MavenArtifactKey(groupId, artifactId, "jar", classifier), moduleConfigurations);
                        if (beforeParent) {
                            beforeParentController.addBeforeParent(groupId, artifactId);
                        }
                    }

                };
            }

            @Override
            public void setDependenciesExcluded(final boolean dependenciesExcluded) {
                this.dependenciesExcluded = dependenciesExcluded;
            }

            @Override
            public void addExcludeWithParents(final String groupId, final String artifactId) {
                if (excludesWithParents == null) {
                    excludesWithParents = new HashSet<MavenArtifactKey>();
                }
                excludesWithParents.add(new MavenArtifactKey(groupId, artifactId, "jar", ""));
            }

            @Override
            public void addExclude(final String groupId, final String artifactId) {
                if (excludes == null) {
                    excludes = new HashSet<MavenArtifactKey>();
                }
                excludes.add(new MavenArtifactKey(groupId, artifactId, "jar", ""));
            }
        };
    }

}
