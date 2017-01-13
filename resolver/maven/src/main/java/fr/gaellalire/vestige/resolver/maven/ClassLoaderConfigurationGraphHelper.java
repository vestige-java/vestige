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
import org.eclipse.aether.impl.DependencyModifier;
import org.eclipse.aether.internal.impl.DefaultDependencyCollector.PremanagedDependency;
import org.eclipse.aether.resolution.ArtifactDescriptorException;
import org.eclipse.aether.resolution.ArtifactDescriptorRequest;
import org.eclipse.aether.resolution.ArtifactDescriptorResult;

/**
 * @author Gael Lalire
 */
public class ClassLoaderConfigurationGraphHelper implements GraphHelper<NodeAndState, MavenArtifact, ClassLoaderConfigurationFactory> {

    private String appName;

    private Map<List<MavenArtifact>, ClassLoaderConfigurationFactory> cachedClassLoaderConfigurationFactory;

    private Map<MavenArtifact, URL> urlByKey;

    private ArtifactDescriptorReader descriptorReader;

    private CollectRequest collectRequest;

    private RepositorySystemSession session;

    private Map<String, Map<String, MavenArtifact>> runtimeDependencies;

    private Scope scope;

    private DependencyModifier dependencyModifier;

    private ScopeModifier scopeModifier;

    public ClassLoaderConfigurationGraphHelper(final String appName, final Map<MavenArtifact, URL> urlByKey, final ArtifactDescriptorReader descriptorReader,
            final CollectRequest collectRequest, final RepositorySystemSession session, final DependencyModifier dependencyModifier,
            final Map<String, Map<String, MavenArtifact>> runtimeDependencies, final Scope scope, final ScopeModifier scopeModifier) {
        cachedClassLoaderConfigurationFactory = new HashMap<List<MavenArtifact>, ClassLoaderConfigurationFactory>();
        this.appName = appName;
        this.urlByKey = urlByKey;
        this.descriptorReader = descriptorReader;
        this.collectRequest = collectRequest;
        this.session = session;
        this.dependencyModifier = dependencyModifier;
        this.runtimeDependencies = runtimeDependencies;
        this.scope = scope;
        this.scopeModifier = scopeModifier;
    }

    /**
     * Executed before any call to {@link ClassLoaderConfigurationFactory#create(fr.gaellalire.vestige.platform.StringParserFactory)}.
     */
    public ClassLoaderConfigurationFactory merge(final List<MavenArtifact> nodes, final List<ClassLoaderConfigurationFactory> nexts) throws Exception {
        List<MavenClassLoaderConfigurationKey> dependencies = new ArrayList<MavenClassLoaderConfigurationKey>();

        for (ClassLoaderConfigurationFactory classLoaderConfiguration : nexts) {
            dependencies.add(classLoaderConfiguration.getKey());
        }

        MavenClassLoaderConfigurationKey key = new MavenClassLoaderConfigurationKey(nodes, dependencies, Scope.PLATFORM);
        ClassLoaderConfigurationFactory classLoaderConfigurationFactory = cachedClassLoaderConfigurationFactory.get(key.getArtifacts());
        // if artifacts are the same, dependencies too. With same artifacts dependencies can differ if they have different mavenConfig (not same application)
        if (classLoaderConfigurationFactory == null) {
            URL[] urls = new URL[nodes.size()];
            int i = 0;
            Scope scope = this.scope;
            for (MavenArtifact artifact : nodes) {
                urls[i] = urlByKey.get(artifact);
                i++;
                if (scopeModifier != null) {
                    scope = scopeModifier.modify(scope, artifact.getGroupId(), artifact.getArtifactId());
                }
            }
            classLoaderConfigurationFactory = new ClassLoaderConfigurationFactory(appName, key, scope, urls, nexts);
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

    public List<NodeAndState> getNexts(final NodeAndState nodeAndState) {

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

            List<Dependency> dependencies = descriptorResult.getDependencies();
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
            throw new RuntimeException(e);
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
