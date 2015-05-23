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
import org.eclipse.aether.collection.DependencySelector;
import org.eclipse.aether.graph.DefaultDependencyNode;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.impl.ArtifactDescriptorReader;
import org.eclipse.aether.internal.impl.DependencyModifier;
import org.eclipse.aether.internal.impl.ModifiedDependencyCollector.PremanagedDependency;
import org.eclipse.aether.resolution.ArtifactDescriptorException;
import org.eclipse.aether.resolution.ArtifactDescriptorRequest;
import org.eclipse.aether.resolution.ArtifactDescriptorResult;

/**
 * @author Gael Lalire
 */
public class ClassLoaderConfigurationGraphHelper implements GraphHelper<NodeAndState, MavenArtifact, ClassLoaderConfigurationFactory> {

    private String appName;

    private Map<MavenClassLoaderConfigurationKey, ClassLoaderConfigurationFactory> cachedClassLoaderConfigurationFactory;

    private Map<MavenArtifact, URL> urlByKey;

    private ArtifactDescriptorReader descriptorReader;

    private CollectRequest collectRequest;

    private RepositorySystemSession session;

    private Map<String, Map<String, MavenArtifact>> runtimeDependencies;

    private Scope scope;

    private DependencySelector dependencySelector;

    private DependencyModifier dependencyModifier;

    public ClassLoaderConfigurationGraphHelper(final String appName, final Map<MavenArtifact, URL> urlByKey,
            final ArtifactDescriptorReader descriptorReader, final CollectRequest collectRequest,
            final RepositorySystemSession session, final DependencyModifier dependencyModifier,
            final Map<String, Map<String, MavenArtifact>> runtimeDependencies, final Scope scope) {
        cachedClassLoaderConfigurationFactory = new HashMap<MavenClassLoaderConfigurationKey, ClassLoaderConfigurationFactory>();
        this.appName = appName;
        this.urlByKey = urlByKey;
        this.descriptorReader = descriptorReader;
        this.collectRequest = collectRequest;
        this.session = session;
        dependencySelector = session.getDependencySelector();
        this.dependencyModifier = dependencyModifier;
        this.runtimeDependencies = runtimeDependencies;
        this.scope = scope;
    }

    public ClassLoaderConfigurationFactory merge(final List<MavenArtifact> nodes,
            final List<ClassLoaderConfigurationFactory> nexts) {
        List<MavenClassLoaderConfigurationKey> dependencies = new ArrayList<MavenClassLoaderConfigurationKey>();

        for (ClassLoaderConfigurationFactory classLoaderConfiguration : nexts) {
            dependencies.add(classLoaderConfiguration.getKey());
        }

        MavenClassLoaderConfigurationKey key = new MavenClassLoaderConfigurationKey(nodes, dependencies, true);
        ClassLoaderConfigurationFactory classLoaderConfigurationFactory = cachedClassLoaderConfigurationFactory.get(key);
        if (classLoaderConfigurationFactory == null) {
            URL[] urls = new URL[nodes.size()];
            int i = 0;
            for (MavenArtifact artifact : nodes) {
                urls[i] = urlByKey.get(artifact);
                i++;
            }
            classLoaderConfigurationFactory = new ClassLoaderConfigurationFactory(appName, key, scope, urls, nexts);
            cachedClassLoaderConfigurationFactory.put(key, classLoaderConfigurationFactory);
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
            DefaultDependencyCollectionContext context = new DefaultDependencyCollectionContext(session, null, nodeAndState.getDependencyNode().getDependency(), managedDependencies);
            // use root dependencySelector to avoid deletion of already included dependency
            DependencySelector dependencySelector = this.dependencySelector.deriveChildSelector(context);
            DependencyManager dependencyManager = nodeAndState.getDependencyManager().deriveChildManager(context);

            List<Dependency> dependencies = descriptorResult.getDependencies();
            ListIterator<Dependency> dependencyIterator = dependencies.listIterator();
            while (dependencyIterator.hasNext()) {
                if (!dependencySelector.selectDependency(dependencyIterator.next())) {
                    dependencyIterator.remove();
                }
            }

            dependencies = dependencyModifier.modify(nodeAndState.getDependencyNode().getDependency(), dependencies);
            List<NodeAndState> children = new ArrayList<NodeAndState>(dependencies.size());
            for (Dependency dependency : dependencies) {
                boolean optional = dependency.isOptional();
                PremanagedDependency preManaged = PremanagedDependency.create(dependencyManager, dependency, false, false);
                dependency = preManaged.managedDependency;
                children.add(new NodeAndState(managedDependencies, new DefaultDependencyNode(dependency), dependencyManager, optional));
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
            if (nodeAndState.isOptional() || nodeAndState.getDependencyNode().getDependency().isOptional()) {
                return null;
            } else {
                throw new RuntimeException(nodeAndState.getDependencyNode().getDependency() + " has no classloader conf");
            }
        }

        return key;
    }

}
