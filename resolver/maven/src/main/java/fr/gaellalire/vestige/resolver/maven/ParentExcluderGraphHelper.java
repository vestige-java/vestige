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

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.aether.artifact.Artifact;

import fr.gaellalire.vestige.spi.resolver.ResolverException;

/**
 * @author Gael Lalire
 */
public class ParentExcluderGraphHelper implements GraphHelper<NodeAndState, DefaultMavenArtifact, Set<MavenArtifactKey>> {

    private DependencyReader dependencyReader;

    private Map<String, Map<String, DefaultMavenArtifact>> runtimeDependencies;

    public ParentExcluderGraphHelper(final DependencyReader dependencyReader, final Map<String, Map<String, DefaultMavenArtifact>> runtimeDependencies) {
        this.dependencyReader = dependencyReader;
        this.runtimeDependencies = runtimeDependencies;
    }

    @Override
    public List<Set<MavenArtifactKey>> merge(final List<DefaultMavenArtifact> pnodes, final List<Set<MavenArtifactKey>> nexts, final boolean mergeIfNoNode,
            final ParentNodeExcluder parentNodeExcluder) throws ResolverException {

        Set<MavenArtifactKey> nodes = new HashSet<MavenArtifactKey>(pnodes.size());
        for (DefaultMavenArtifact mavenArtifact : pnodes) {
            if (!mavenArtifact.isVirtual()) {
                nodes.add(new MavenArtifactKey(mavenArtifact.getGroupId(), mavenArtifact.getArtifactId(), mavenArtifact.getExtension()));
            } else if (mavenArtifact.isParentExcluder()) {
                parentNodeExcluder.setExcludeParentNodes();
            }
        }

        if (nodes.size() == 0 && !mergeIfNoNode) {
            return nexts;
        }

        for (Set<MavenArtifactKey> depSet : nexts) {
            nodes.addAll(depSet);
        }

        return Collections.singletonList(nodes);
    }

    @Override
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

    @Override
    public List<NodeAndState> getNexts(final NodeAndState nodeAndState) throws ResolverException {
        return dependencyReader.getDependencies(nodeAndState);
    }

}
