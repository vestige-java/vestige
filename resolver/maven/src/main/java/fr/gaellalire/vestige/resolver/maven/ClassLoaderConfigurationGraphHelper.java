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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.DependencyManager;
import org.eclipse.aether.graph.DefaultDependencyNode;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.impl.ArtifactDescriptorReader;
import org.eclipse.aether.internal.impl.DefaultDependencyCollector.PremanagedDependency;
import org.eclipse.aether.resolution.ArtifactDescriptorException;
import org.eclipse.aether.resolution.ArtifactDescriptorRequest;
import org.eclipse.aether.resolution.ArtifactDescriptorResult;

import fr.gaellalire.vestige.jpms.NamedModuleUtils;
import fr.gaellalire.vestige.platform.JPMSClassLoaderConfiguration;
import fr.gaellalire.vestige.platform.JPMSNamedModulesConfiguration;
import fr.gaellalire.vestige.platform.ModuleConfiguration;
import fr.gaellalire.vestige.platform.SecureFile;
import fr.gaellalire.vestige.spi.resolver.ResolverException;
import fr.gaellalire.vestige.spi.resolver.Scope;

/**
 * @author Gael Lalire
 */
public class ClassLoaderConfigurationGraphHelper implements GraphHelper<NodeAndState, MavenArtifact, ClassLoaderConfigurationFactory> {

    private String appName;

    private Map<List<MavenArtifact>, ClassLoaderConfigurationFactory> cachedClassLoaderConfigurationFactory;

    private Map<MavenArtifact, File> urlByKey;

    private ArtifactDescriptorReader descriptorReader;

    private CollectRequest collectRequest;

    private RepositorySystemSession session;

    private Map<String, Map<String, MavenArtifact>> runtimeDependencies;

    private Scope scope;

    private DefaultDependencyModifier dependencyModifier;

    private DefaultJPMSConfiguration jpmsConfiguration;

    private ScopeModifier scopeModifier;

    private JPMSNamedModulesConfiguration namedModulesConfiguration;

    public ClassLoaderConfigurationGraphHelper(final String appName, final Map<MavenArtifact, File> urlByKey, final ArtifactDescriptorReader descriptorReader,
            final CollectRequest collectRequest, final RepositorySystemSession session, final DefaultDependencyModifier dependencyModifier,
            final DefaultJPMSConfiguration jpmsConfiguration, final Map<String, Map<String, MavenArtifact>> runtimeDependencies, final Scope scope,
            final ScopeModifier scopeModifier, final JPMSNamedModulesConfiguration namedModulesConfiguration) {
        cachedClassLoaderConfigurationFactory = new HashMap<List<MavenArtifact>, ClassLoaderConfigurationFactory>();
        this.appName = appName;
        this.urlByKey = urlByKey;
        this.descriptorReader = descriptorReader;
        this.collectRequest = collectRequest;
        this.session = session;
        this.dependencyModifier = dependencyModifier;
        this.jpmsConfiguration = jpmsConfiguration;
        this.runtimeDependencies = runtimeDependencies;
        this.scope = scope;
        this.scopeModifier = scopeModifier;
        this.namedModulesConfiguration = namedModulesConfiguration;
    }

    /**
     * Executed before any call to {@link ClassLoaderConfigurationFactory#create(fr.gaellalire.vestige.platform.StringParserFactory)}.
     */
    public ClassLoaderConfigurationFactory merge(final List<MavenArtifact> nodes, final List<ClassLoaderConfigurationFactory> nexts) throws ResolverException {
        List<MavenClassLoaderConfigurationKey> dependencies = new ArrayList<MavenClassLoaderConfigurationKey>();

        for (ClassLoaderConfigurationFactory classLoaderConfiguration : nexts) {
            dependencies.add(classLoaderConfiguration.getKey());
        }

        JPMSClassLoaderConfiguration moduleConfiguration = JPMSClassLoaderConfiguration.EMPTY_INSTANCE;
        boolean[] beforeParents = null;
        int i = 0;
        for (MavenArtifact mavenArtifact : nodes) {
            JPMSClassLoaderConfiguration unnamedClassLoaderConfiguration = jpmsConfiguration.getModuleConfiguration(mavenArtifact.getGroupId(), mavenArtifact.getArtifactId());
            if (namedModulesConfiguration != null) {

                String moduleName;
                try {
                    moduleName = NamedModuleUtils.getModuleName(urlByKey.get(mavenArtifact));
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
            if (dependencyModifier.isBeforeParent(mavenArtifact.getGroupId(), mavenArtifact.getArtifactId())) {
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
            List<SecureFile> beforeUrls = new ArrayList<SecureFile>();
            List<SecureFile> afterUrls = new ArrayList<SecureFile>();
            Scope scope = this.scope;
            i = 0;
            for (MavenArtifact artifact : nodes) {
                if (scopeModifier != null) {
                    scope = scopeModifier.modify(scope, artifact.getGroupId(), artifact.getArtifactId());
                }

                List<SecureFile> urls;
                if (beforeParents != null && beforeParents[i]) {
                    urls = beforeUrls;
                } else {
                    urls = afterUrls;
                }
                try {
                    urls.add(new SecureFile(urlByKey.get(artifact), new URL(artifact.toString()), artifact.getSha1sum()));
                } catch (MalformedURLException e) {
                    throw new ResolverException("Unable to create Maven URL", e);
                }
                i++;
            }
            classLoaderConfigurationFactory = new ClassLoaderConfigurationFactory(appName, key, scope, beforeUrls, afterUrls, nexts, namedModulesConfiguration != null);
            cachedClassLoaderConfigurationFactory.put(key.getArtifacts(), classLoaderConfigurationFactory);
        }
        return classLoaderConfigurationFactory;
    }

    private List<Dependency> mergeDeps(final List<Dependency> dominant, final List<Dependency> recessive) {
        List<Dependency> result;
        if (dominant == null || dominant.isEmpty()) {
            result = recessive;
        } else if (recessive == null || recessive.isEmpty()) {
            result = dominant;
        } else {
            int initialCapacity = dominant.size() + recessive.size();
            result = new ArrayList<Dependency>(initialCapacity);
            Collection<String> ids = new HashSet<String>(initialCapacity, 1.0f);
            for (Dependency dependency : dominant) {
                ids.add(getId(dependency.getArtifact()));
                result.add(dependency);
            }
            for (Dependency dependency : recessive) {
                if (!ids.contains(getId(dependency.getArtifact()))) {
                    result.add(dependency);
                }
            }
        }
        return result;
    }

    private static String getId(final Artifact a) {
        return a.getGroupId() + ':' + a.getArtifactId() + ':' + a.getClassifier() + ':' + a.getExtension();
    }

    public List<NodeAndState> getNexts(final NodeAndState nodeAndState) throws ResolverException {

        collectRequest.setRoot(nodeAndState.getDependencyNode().getDependency());
        try {
            ArtifactDescriptorRequest descriptorRequest = new ArtifactDescriptorRequest();
            descriptorRequest.setArtifact(nodeAndState.getDependencyNode().getArtifact());
            descriptorRequest.setRepositories(collectRequest.getRepositories());
            descriptorRequest.setRequestContext(collectRequest.getRequestContext());
            ArtifactDescriptorResult descriptorResult = descriptorReader.readArtifactDescriptor(session, descriptorRequest);
            List<Dependency> managedDependencies = mergeDeps(nodeAndState.getManagedDependencies(), descriptorResult.getManagedDependencies());
            DefaultDependencyCollectionContext context = new DefaultDependencyCollectionContext(session, null, nodeAndState.getDependencyNode().getDependency(),
                    managedDependencies);
            DependencyManager dependencyManager = nodeAndState.getDependencyManager().deriveChildManager(context);

            List<Dependency> dependencies = new ArrayList<Dependency>(descriptorResult.getDependencies());
            ListIterator<Dependency> dependencyIterator = dependencies.listIterator();
            // remove only test scope (provided and optional are ignored only if not repeated by another dependency)
            while (dependencyIterator.hasNext()) {
                if ("test".equals(dependencyIterator.next().getScope())) {
                    dependencyIterator.remove();
                }
            }

            dependencies = dependencyModifier.modify(nodeAndState.getDependencyNode().getDependency(), dependencies);
            List<NodeAndState> children = new ArrayList<NodeAndState>(dependencies.size());
            for (Dependency dependency : dependencies) {
                PremanagedDependency preManaged = PremanagedDependency.create(dependencyManager, dependency, false, false);
                dependency = preManaged.getManagedDependency();
                children.add(new NodeAndState(managedDependencies, new DefaultDependencyNode(dependency), dependencyManager));
            }
            return children;
        } catch (ArtifactDescriptorException e) {
            throw new ResolverException(e);
        }
    }

    public MavenArtifact getKey(final NodeAndState nodeAndState) {
        Artifact artifact = nodeAndState.getDependencyNode().getDependency().getArtifact();

        MavenArtifact key = null;
        Map<String, MavenArtifact> map = runtimeDependencies.get(artifact.getGroupId());
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
