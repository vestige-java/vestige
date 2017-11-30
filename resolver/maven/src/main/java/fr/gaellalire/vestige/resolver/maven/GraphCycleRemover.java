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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import fr.gaellalire.vestige.spi.resolver.ResolverException;

/**
 * @param <Node> type of input nodes
 * @param <Key> type of input node key
 * @param <RNode> type of output nodes
 * @author Gael Lalire
 */
public class GraphCycleRemover<Node, Key, RNode> {

    private GraphHelper<Node, Key, RNode> graphHelper;

    public GraphCycleRemover(final GraphHelper<Node, Key, RNode> graphHelper) {
        this.graphHelper = graphHelper;
    }

    /**
     * @author Gael Lalire
     */
    class Context {

        private Map<Key, List<RNode>> cache = new HashMap<Key, List<RNode>>();

        private List<List<Key>> merge = new LinkedList<List<Key>>();

        private List<Key> parents = new LinkedList<Key>();

        public boolean pushNode(final Key key) {
            int indexOf = parents.indexOf(key);
            if (indexOf != -1) {
                // more in cycle

                ListIterator<List<Key>> listIterator = merge.listIterator(indexOf + 1);
                List<Key> list = listIterator.previous();
                if (list.size() == 1) {
                    list = new ArrayList<Key>(list);
                    listIterator.set(list);
                }
                while (listIterator.hasPrevious()) {
                    List<Key> previous = listIterator.previous();
                    for (Key pkey : previous) {
                        if (!list.contains(pkey)) {
                            list.add(pkey);
                        }
                    }
                    listIterator.set(list);
                }

                return false;
            }

            parents.add(0, key);
            merge.add(0, Collections.singletonList(key));
            return true;
        }

        public List<Key> popNode() {
            parents.remove(0);
            List<Key> list = merge.remove(0);
            if (merge.size() != 0 && list == merge.get(0)) {
                return null;
            }
            return list;
        }

        public void putCache(final Key key, final List<RNode> result) {
            cache.put(key, result);
        }

        public List<RNode> getCache(final Key key) {
            return cache.get(key);
        }

    }

    public RNode removeCycle(final Node node) throws ResolverException {
        return removeCycle(node, new Context()).get(0);
    }

    public List<RNode> removeCycle(final Node node, final Context context) throws ResolverException {
        Key key = graphHelper.getKey(node);

        if (key == null) {
            return Collections.emptyList();
        }

        List<RNode> result = context.getCache(key);
        if (result != null) {
            return result;
        }

        if (!context.pushNode(key)) {
            return Collections.emptyList();
        }
        List<Node> nexts = graphHelper.getNexts(node);
        List<RNode> mergedNexts = new ArrayList<RNode>();
        for (Node next : nexts) {
            List<RNode> removeCycle = removeCycle(next, context);
            for (RNode rNode : removeCycle) {
                if (!mergedNexts.contains(rNode)) {
                    mergedNexts.add(rNode);
                }
            }
        }

        List<Key> popNode = context.popNode();
        if (popNode == null) {
            // we are in cycle
            return mergedNexts;
        }

        result = Collections.singletonList(graphHelper.merge(popNode, mergedNexts));
        for (Key sharedKey : popNode) {
            context.putCache(sharedKey, result);
        }
        return result;
    }

}
