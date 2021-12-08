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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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

import fr.gaellalire.vestige.spi.resolver.ResolverException;

/**
 * @author Gael Lalire
 */
public class DependencyReader {

    private ArtifactDescriptorReader descriptorReader;

    private CollectRequest collectRequest;

    private RepositorySystemSession session;

    private DependencyModifier dependencyModifier;

    public DependencyReader(final ArtifactDescriptorReader descriptorReader, final CollectRequest collectRequest, final RepositorySystemSession session,
            final DependencyModifier dependencyModifier) {
        this.descriptorReader = descriptorReader;
        this.collectRequest = collectRequest;
        this.session = session;
        this.dependencyModifier = dependencyModifier;
    }

    private static String getId(final Artifact a) {
        return a.getGroupId() + ':' + a.getArtifactId() + ':' + a.getClassifier() + ':' + a.getExtension();
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

    public List<MavenArtifactAndMetadata> retainDependencies(final NodeAndState nodeAndState, final List<MavenArtifactAndMetadata> artifactAndMetadatas) throws ResolverException {
        List<MavenArtifactAndMetadata> result = new ArrayList<MavenArtifactAndMetadata>();
        Map<String, Map<String, MavenArtifactAndMetadata>> runtimeDependencies = new HashMap<String, Map<String, MavenArtifactAndMetadata>>();
        for (MavenArtifactAndMetadata artifact : artifactAndMetadatas) {
            DefaultMavenArtifact mavenArtifact = artifact.getMavenArtifact();
            Map<String, MavenArtifactAndMetadata> map = runtimeDependencies.get(mavenArtifact.getGroupId());
            if (map == null) {
                map = new HashMap<String, MavenArtifactAndMetadata>();
                runtimeDependencies.put(mavenArtifact.getGroupId(), map);
            }
            map.put(mavenArtifact.getArtifactId(), artifact);
        }
        Deque<Iterator<NodeAndState>> stack = new ArrayDeque<Iterator<NodeAndState>>();
        Iterator<NodeAndState> current = Collections.singletonList(nodeAndState).iterator();
        stack.push(current);
        while (stack.size() != 0) {
            if (current.hasNext()) {
                NodeAndState nodeAndState2 = current.next();
                Artifact artifact = nodeAndState2.getDependencyNode().getArtifact();
                Map<String, MavenArtifactAndMetadata> map = runtimeDependencies.get(artifact.getGroupId());
                if (map != null) {
                    // map can be null because provided dependencies are not included
                    MavenArtifactAndMetadata mavenArtifact = map.remove(artifact.getArtifactId());
                    if (mavenArtifact != null) {
                        result.add(mavenArtifact);
                        stack.push(current);
                        current = getDependencies(nodeAndState2).iterator();
                    }
                }
            } else {
                current = stack.pop();
            }
        }

        return result;
    }

    public List<NodeAndState> getDependencies(final NodeAndState nodeAndState) throws ResolverException {
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
                children.add(new NodeAndState(false, managedDependencies, new DefaultDependencyNode(dependency), dependencyManager));
            }
            return children;
        } catch (ArtifactDescriptorException e) {
            throw new ResolverException(e);
        }

    }

}
