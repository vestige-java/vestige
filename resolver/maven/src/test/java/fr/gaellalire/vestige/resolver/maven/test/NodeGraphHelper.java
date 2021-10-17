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

package fr.gaellalire.vestige.resolver.maven.test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import fr.gaellalire.vestige.resolver.maven.GraphHelper;

/**
 * @author Gael Lalire
 */
public class NodeGraphHelper implements GraphHelper<Node, Node, Node> {

    public List<Node> merge(final List<Node> nodes, final List<Node> nexts, final boolean mergeIfNoNode, final ParentNodeExcluder parentNodeExcluder) {
        List<String> names = new ArrayList<String>();
        boolean parentExcluded = false;
        for (Node node : nodes) {
            if (node.isParentExcluded()) {
                parentExcluded = true;
                names.clear();
                break;
            }
            if (!node.isExcluded()) {
                names.addAll(node.getNames());
            }
        }
        for (Node next : nexts) {
            if (next.isParentExcluded()) {
                names.clear();
                parentExcluded = true;
            }
        }
        if (names.size() == 0 && !mergeIfNoNode) {
            parentNodeExcluder.setExcludeParentNodes();
            return nexts;
        }

        nexts.removeAll(nodes);
        Node node = new Node(names);
        node.setNexts(nexts);
        node.setParentExcluded(parentExcluded);
        return Collections.singletonList(node);
    }

    public List<Node> getNexts(final Node node) {
        return node.getNexts();
    }

    public Node getKey(final Node node) {
        return node;
    }

}
