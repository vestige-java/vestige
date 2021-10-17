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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.aether.artifact.Artifact;

import fr.gaellalire.vestige.jpms.NamedModuleUtils;
import fr.gaellalire.vestige.platform.FileWithMetadata;
import fr.gaellalire.vestige.platform.JPMSClassLoaderConfiguration;
import fr.gaellalire.vestige.platform.JPMSNamedModulesConfiguration;
import fr.gaellalire.vestige.platform.ModuleConfiguration;
import fr.gaellalire.vestige.spi.resolver.ResolverException;
import fr.gaellalire.vestige.spi.resolver.Scope;

/**
 * @author Gael Lalire
 */
public class ClassLoaderConfigurationGraphHelper implements GraphHelper<NodeAndState, DefaultMavenArtifact, ClassLoaderConfigurationFactory> {

    private String appName;

    private Map<List<DefaultMavenArtifact>, ClassLoaderConfigurationFactory> cachedClassLoaderConfigurationFactory;

    private Map<DefaultMavenArtifact, FileWithMetadata> urlByKey;

    private Map<String, Map<String, DefaultMavenArtifact>> runtimeDependencies;

    private Scope scope;

    private BeforeParentController beforeParentController;

    private DefaultJPMSConfiguration jpmsConfiguration;

    private ScopeModifier scopeModifier;

    private JPMSNamedModulesConfiguration namedModulesConfiguration;

    private DependencyReader dependencyReader;

    private List<DefaultMavenArtifact> mavenArtifacts;

    public ClassLoaderConfigurationGraphHelper(final String appName, final Map<DefaultMavenArtifact, FileWithMetadata> urlByKey, final DependencyReader dependencyReader,
            final BeforeParentController beforeParentController, final DefaultJPMSConfiguration jpmsConfiguration,
            final Map<String, Map<String, DefaultMavenArtifact>> runtimeDependencies, final Scope scope, final ScopeModifier scopeModifier,
            final JPMSNamedModulesConfiguration namedModulesConfiguration, final List<DefaultMavenArtifact> mavenArtifacts) {
        cachedClassLoaderConfigurationFactory = new HashMap<List<DefaultMavenArtifact>, ClassLoaderConfigurationFactory>();
        this.appName = appName;
        this.urlByKey = urlByKey;
        this.dependencyReader = dependencyReader;
        this.beforeParentController = beforeParentController;
        this.jpmsConfiguration = jpmsConfiguration;
        this.runtimeDependencies = runtimeDependencies;
        this.scope = scope;
        this.scopeModifier = scopeModifier;
        this.namedModulesConfiguration = namedModulesConfiguration;
        this.mavenArtifacts = mavenArtifacts;
    }

    /**
     * Executed before any call to {@link ClassLoaderConfigurationFactory#create(fr.gaellalire.vestige.platform.StringParserFactory)}.
     */
    public List<ClassLoaderConfigurationFactory> merge(final List<DefaultMavenArtifact> pnodes, final List<ClassLoaderConfigurationFactory> nexts, final boolean mergeIfNoNode,
            final ParentNodeExcluder parentNodeExcluder) throws ResolverException {
        List<MavenClassLoaderConfigurationKey> dependencies = new ArrayList<MavenClassLoaderConfigurationKey>();

        List<DefaultMavenArtifact> nodes = new ArrayList<DefaultMavenArtifact>(pnodes.size());
        for (DefaultMavenArtifact mavenArtifact : pnodes) {
            if (!mavenArtifact.isVirtual()) {
                nodes.add(mavenArtifact);
                mavenArtifacts.add(mavenArtifact);
            } else if (mavenArtifact.isParentExcluder()) {
                parentNodeExcluder.setExcludeParentNodes();
            }
        }

        if (nodes.size() == 0 && !mergeIfNoNode) {
            return nexts;
        }

        for (ClassLoaderConfigurationFactory classLoaderConfiguration : nexts) {
            dependencies.add(classLoaderConfiguration.getKey());
        }

        JPMSClassLoaderConfiguration moduleConfiguration = JPMSClassLoaderConfiguration.EMPTY_INSTANCE;
        boolean[] beforeParents = null;
        int i = 0;
        for (DefaultMavenArtifact mavenArtifact : nodes) {
            JPMSClassLoaderConfiguration unnamedClassLoaderConfiguration = jpmsConfiguration.getModuleConfiguration(mavenArtifact.getGroupId(), mavenArtifact.getArtifactId());
            if (namedModulesConfiguration != null) {

                String moduleName;
                try {
                    moduleName = NamedModuleUtils.getModuleName(urlByKey.get(mavenArtifact).getFile());
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
            if (beforeParentController.isBeforeParent(mavenArtifact.getGroupId(), mavenArtifact.getArtifactId())) {
                if (beforeParents == null) {
                    beforeParents = new boolean[nodes.size()];
                }
                beforeParents[i] = true;
            }
            i++;
        }

        MavenClassLoaderConfigurationKey key = new MavenClassLoaderConfigurationKey(nodes, dependencies, Scope.PLATFORM, moduleConfiguration, namedModulesConfiguration,
                beforeParents);
        ClassLoaderConfigurationFactory classLoaderConfigurationFactory = cachedClassLoaderConfigurationFactory.get(key.getArtifacts());
        // if artifacts are the same, dependencies too. With same artifacts dependencies can differ if they have different mavenConfig (not same application)
        if (classLoaderConfigurationFactory == null) {
            List<FileWithMetadata> beforeUrls = new ArrayList<FileWithMetadata>();
            List<FileWithMetadata> afterUrls = new ArrayList<FileWithMetadata>();
            Scope scope = this.scope;
            i = 0;
            for (DefaultMavenArtifact artifact : nodes) {
                if (scopeModifier != null) {
                    scope = scopeModifier.modify(scope, artifact.getGroupId(), artifact.getArtifactId());
                }

                List<FileWithMetadata> urls;
                if (beforeParents != null && beforeParents[i]) {
                    urls = beforeUrls;
                } else {
                    urls = afterUrls;
                }
                urls.add(urlByKey.get(artifact));
                i++;
            }
            classLoaderConfigurationFactory = new ClassLoaderConfigurationFactory(appName, key, scope, beforeUrls, afterUrls, nexts, namedModulesConfiguration != null);
            cachedClassLoaderConfigurationFactory.put(key.getArtifacts(), classLoaderConfigurationFactory);
        }
        return Collections.singletonList(classLoaderConfigurationFactory);
    }

    public List<NodeAndState> getNexts(final NodeAndState nodeAndState) throws ResolverException {
        return dependencyReader.getDependencies(nodeAndState);
    }

    public DefaultMavenArtifact getKey(final NodeAndState nodeAndState) {
        Artifact artifact = nodeAndState.getDependencyNode().getDependency().getArtifact();

        DefaultMavenArtifact key = null;
        Map<String, DefaultMavenArtifact> map = runtimeDependencies.get(artifact.getGroupId());
        if (map != null) {
            key = map.get(artifact.getArtifactId());
        }
        if (key == null) {
            // key not known because dependency is optional, dependency is test
            // scope for root, dependency is excluded by an exclude element
            return null;
        }
        return key;
    }

}
