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

package com.googlecode.vestige.resolver.maven.test;

import java.util.ArrayList;
import java.util.List;

import com.googlecode.vestige.resolver.maven.GraphHelper;

/**
 * @author Gael Lalire
 */
public class NodeGraphHelper implements GraphHelper<Node, Node, Node> {

    public Node merge(final List<Node> nodes, final List<Node> nexts) {
        List<String> names = new ArrayList<String>();
        for (Node node : nodes) {
            names.addAll(node.getNames());
        }
        nexts.removeAll(nodes);
        Node node = new Node(names);
        node.setNexts(nexts);
        return node;
    }

    public List<Node> getNexts(final Node node) {
        return node.getNexts();
    }

    public Node getKey(final Node node) {
        return node;
    }

}
