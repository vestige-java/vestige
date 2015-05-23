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

import java.util.List;

import org.eclipse.aether.collection.DependencyManager;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;

/**
 * @author Gael Lalire
 */
public class NodeAndState {

    private List<Dependency> managedDependencies;

    private DependencyNode dependencyNode;

    private DependencyManager dependencyManager;

    private boolean optional;

    public NodeAndState(final List<Dependency> managedDependencies, final DependencyNode dependencyNode, final DependencyManager dependencyManager, final boolean optional) {
        this.managedDependencies = managedDependencies;
        this.dependencyNode = dependencyNode;
        this.dependencyManager = dependencyManager;
        this.optional = optional;
    }

    public DependencyNode getDependencyNode() {
        return dependencyNode;
    }

    public List<Dependency> getManagedDependencies() {
        return managedDependencies;
    }

    public DependencyManager getDependencyManager() {
        return dependencyManager;
    }

    public boolean isOptional() {
        return optional;
    }

    @Override
    public String toString() {
        return dependencyNode.toString();
    }

}
