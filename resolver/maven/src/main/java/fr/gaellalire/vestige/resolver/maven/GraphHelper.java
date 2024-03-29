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

import fr.gaellalire.vestige.spi.resolver.ResolverException;

/**
 * @param <Node> type of input nodes
 * @param <Key> type of input node key
 * @param <RNode> type of output nodes
 * @author Gael Lalire
 */
public interface GraphHelper<Node, Key, RNode> {

    /**
     * @author Gael Lalire
     */
    interface ParentNodeExcluder {

        void setExcludeParentNodes();

    }

    List<RNode> merge(List<Key> nodes, List<RNode> nexts, boolean mergeIfNoNode, ParentNodeExcluder parentNodeExcluder) throws ResolverException;

    Key getKey(Node node);

    List<Node> getNexts(Node node) throws ResolverException;

}
