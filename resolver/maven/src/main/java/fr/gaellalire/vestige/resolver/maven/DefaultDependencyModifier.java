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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.impl.DependencyModifier;

/**
 * @author Gael Lalire
 */
public class DefaultDependencyModifier implements BeforeParentController, DependencyModifier, ArtifactPatcher {

    private Map<MavenArtifactKey, ReplacementRule> replacementRules = new HashMap<MavenArtifactKey, ReplacementRule>();

    private Map<MavenArtifactKey, List<Dependency>> addRules = new HashMap<MavenArtifactKey, List<Dependency>>();

    private Map<MavenArtifactKey, List<MavenArtifactKey>> removeRules = new HashMap<MavenArtifactKey, List<MavenArtifactKey>>();

    private Map<MavenArtifactKey, Artifact> patches = new HashMap<MavenArtifactKey, Artifact>();

    private Map<String, Set<String>> beforeParents = new HashMap<String, Set<String>>();

    private Map<String, SetClassifierRule> classifierByExtension = new HashMap<String, SetClassifierRule>();

    public void setClassifierToExtension(final String extension, final String classifier, final Set<MavenArtifactKey> excepts) {
        classifierByExtension.put(extension, new SetClassifierRule(classifier, excepts));
    }

    public void replace(final MavenArtifactKey mavenArtifactKey, final List<Dependency> dependency, final Set<MavenArtifactKey> excepts) {
        replacementRules.put(mavenArtifactKey, new ReplacementRule(dependency, excepts));
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

    public void remove(final MavenArtifactKey mavenArtifactKey, final List<MavenArtifactKey> dependency) {
        removeRules.put(mavenArtifactKey, dependency);
    }

    public void add(final MavenArtifactKey mavenArtifactKey, final List<Dependency> dependency) {
        addRules.put(mavenArtifactKey, dependency);
    }

    public List<Dependency> replaceDependency(final MavenArtifactKey parentKey, final Dependency dependency) {
        List<Dependency> replacedDependency = null;

        Artifact artifact = dependency.getArtifact();
        MavenArtifactKey key = new MavenArtifactKey(artifact.getGroupId(), artifact.getArtifactId(), artifact.getExtension(), artifact.getClassifier());
        SetClassifierRule setClassifierRule = classifierByExtension.get(artifact.getExtension());
        if (setClassifierRule != null) {
            Set<MavenArtifactKey> excepts = setClassifierRule.getExcepts();
            if (excepts == null || !excepts.contains(key)) {
                artifact = new DefaultArtifact(artifact.getGroupId(), artifact.getArtifactId(), setClassifierRule.getClassifier(), artifact.getExtension(), artifact.getVersion(),
                        artifact.getProperties(), (File) null);
                key = new MavenArtifactKey(artifact.getGroupId(), artifact.getArtifactId(), artifact.getExtension(), artifact.getClassifier());
                replacedDependency = Collections.singletonList(new Dependency(artifact, "runtime"));
            }
        }

        ReplacementRule replacementRule = replacementRules.get(key);
        if (replacementRule != null) {
            if (parentKey != null) {
                Set<MavenArtifactKey> excepts = replacementRule.getExcepts();
                if (excepts != null && excepts.contains(parentKey)) {
                    // excepts
                    return replacedDependency;
                }
            }
            return replacementRule.getAddedDependencies();
        }
        return replacedDependency;
    }

    public List<Dependency> modify(final Dependency dependency, final List<Dependency> children) {
        Artifact parentArtifact = null;
        MavenArtifactKey parentKey = null;
        if (dependency != null) {
            parentArtifact = dependency.getArtifact();
            parentKey = new MavenArtifactKey(parentArtifact.getGroupId(), parentArtifact.getArtifactId(), parentArtifact.getExtension(), parentArtifact.getClassifier());
            List<MavenArtifactKey> rDependency = removeRules.get(parentKey);
            if (rDependency != null) {
                Iterator<Dependency> iterator = children.iterator();
                while (iterator.hasNext()) {
                    Artifact next = iterator.next().getArtifact();
                    if (rDependency.contains(new MavenArtifactKey(next.getGroupId(), next.getArtifactId(), next.getExtension(), next.getClassifier()))) {
                        iterator.remove();
                    }
                }
            }
            List<Dependency> aDependency = addRules.get(parentKey);
            if (aDependency != null) {
                children.addAll(aDependency);
            }
        }
        ListIterator<Dependency> listIterator = children.listIterator();
        while (listIterator.hasNext()) {
            List<Dependency> replaceDependency = replaceDependency(parentKey, listIterator.next());
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

    public void setPatch(final MavenArtifactKey mavenArtifactKey, final Artifact patch) {
        patches.put(mavenArtifactKey, patch);
    }

    @Override
    public Artifact patch(final Artifact mavenArtifact) {
        return patches.get(new MavenArtifactKey(mavenArtifact.getGroupId(), mavenArtifact.getArtifactId(), mavenArtifact.getExtension(), mavenArtifact.getClassifier()));
    }

    @Override
    public Artifact replace(final Artifact artifact) {
        List<Dependency> replacedDependencies = replaceDependency(null, new Dependency(artifact, "runtime"));
        if (replacedDependencies == null || replacedDependencies.size() != 1) {
            return artifact;
        }
        return replacedDependencies.get(0).getArtifact();
    }

}
