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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.impl.DependencyModifier;

/**
 * @author Gael Lalire
 */
public class DefaultDependencyModifier implements BeforeParentController, DependencyModifier {

    private Map<String, Map<String, ReplacementRule>> replacementRules = new HashMap<String, Map<String, ReplacementRule>>();

    private Map<String, Map<String, List<Dependency>>> addRules = new HashMap<String, Map<String, List<Dependency>>>();

    private Map<String, Set<String>> beforeParents = new HashMap<String, Set<String>>();

    public void replace(final String groupId, final String artifactId, final List<Dependency> dependency, final Map<String, Set<String>> excepts) {
        Map<String, ReplacementRule> map = replacementRules.get(groupId);
        if (map == null) {
            map = new HashMap<String, ReplacementRule>();
            replacementRules.put(groupId, map);
        }
        map.put(artifactId, new ReplacementRule(dependency, excepts));
    }

    public void addBeforeParent(final String groupId, final String artifactId) {
        Set<String> set = beforeParents.get(groupId);
        if (set == null) {
            set = new HashSet<String>();
            beforeParents.put(groupId, set);
        }
        set.add(artifactId);
    }

    public boolean isBeforeParent(final String groupId, final String artifactId) {
        Set<String> set = beforeParents.get(groupId);
        if (set == null) {
            return false;
        }
        return set.contains(artifactId);
    }

    public void add(final String groupId, final String artifactId, final List<Dependency> dependency) {
        Map<String, List<Dependency>> map = addRules.get(groupId);
        if (map == null) {
            map = new HashMap<String, List<Dependency>>();
            addRules.put(groupId, map);
        }
        map.put(artifactId, dependency);
    }

    public List<Dependency> replaceDependency(final Artifact parentArtifact, final Dependency dependency) {
        Artifact artifact = dependency.getArtifact();
        Map<String, ReplacementRule> map = replacementRules.get(artifact.getGroupId());
        if (map != null) {
            ReplacementRule replacementRule = map.get(artifact.getArtifactId());
            if (replacementRule != null) {
                if (parentArtifact != null) {
                    Map<String, Set<String>> excepts = replacementRule.getExcepts();
                    if (excepts != null) {
                        Set<String> artifactIds = excepts.get(parentArtifact.getGroupId());
                        if (artifactIds != null && artifactIds.contains(parentArtifact.getArtifactId())) {
                            // excepts
                            return null;
                        }
                    }
                }
                return replacementRule.getAddedDependencies();
            }
        }
        return null;
    }

    public List<Dependency> modify(final Dependency dependency, final List<Dependency> children) {
        Artifact parentArtifact = null;
        if (dependency != null) {
            parentArtifact = dependency.getArtifact();
            Map<String, List<Dependency>> map = addRules.get(parentArtifact.getGroupId());
            if (map != null) {
                List<Dependency> rDependency = map.get(parentArtifact.getArtifactId());
                if (rDependency != null) {
                    children.addAll(rDependency);
                }
            }
        }
        ListIterator<Dependency> listIterator = children.listIterator();
        while (listIterator.hasNext()) {
            List<Dependency> replaceDependency = replaceDependency(parentArtifact, listIterator.next());
            if (replaceDependency != null) {
                listIterator.remove();
                Iterator<Dependency> iterator = replaceDependency.iterator();
                while (iterator.hasNext()) {
                    listIterator.add(iterator.next());
                }
            }
        }
        return children;
    }

}
