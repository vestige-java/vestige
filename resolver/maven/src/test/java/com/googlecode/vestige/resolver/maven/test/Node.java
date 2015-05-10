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

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * @author Gael Lalire
 */
public class Node {

    private List<String> names;

    private List<Node> nexts;

    public Node(final String name) {
        this.names = Collections.singletonList(name);
        nexts = Collections.emptyList();
    }

    public Node(final List<String> names) {
        this.names = names;
        nexts = Collections.emptyList();
    }

    public List<String> getNames() {
        return names;
    }

    public void setNexts(final List<Node> nexts) {
        this.nexts = nexts;
    }

    public List<Node> getNexts() {
        return nexts;
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof Node)) {
            return false;
        }
        Node node = (Node) obj;
        if (!names.equals(node.getNames())) {
            return false;
        }
        if (!nexts.equals(node.getNexts())) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return names.hashCode();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("\n");
        toString(new HashSet<Node>(), builder, 1);
        return builder.toString();
    }

    public void toString(final Set<Node> nodes, final StringBuilder builder, final int indent) {
        builder.append(names.toString());
        if (nodes.add(this)) {
            Iterator<Node> iterator = nexts.iterator();
            while (iterator.hasNext()) {
                builder.append("\n");
                for (int i = 0; i < indent - 1; i++) {
                    builder.append("    ");
                }
                builder.append("|-");
                iterator.next().toString(nodes, builder, indent + 1);
            }
            nodes.remove(this);
        } else {
            builder.append("...loop");
        }
    }

}
