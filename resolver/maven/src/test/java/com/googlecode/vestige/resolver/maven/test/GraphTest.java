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

import java.util.Arrays;
import java.util.Collections;

import org.junit.Assert;
import org.junit.Test;

import com.googlecode.vestige.resolver.maven.GraphCycleRemover;

/**
 * @author Gael Lalire
 */
public class GraphTest {

    private GraphCycleRemover<Node, Node, Node> graphCycleRemover = new GraphCycleRemover<Node, Node, Node>(new NodeGraphHelper());

    @Test
    public void testCycle() {
        Node n1 = new Node("1");
        Node n2 = new Node("2");
        Node n3 = new Node("3");
        Node n4 = new Node("4");
        n1.setNexts(Collections.singletonList(n2));
        n2.setNexts(Collections.singletonList(n3));
        n3.setNexts(Arrays.asList(n2, n4));

        Node r1 = new Node("1");
        Node r2a3 = new Node(Arrays.asList("2", "3"));
        Node r4 = new Node("4");
        r1.setNexts(Collections.singletonList(r2a3));
        r2a3.setNexts(Collections.singletonList(r4));

        Assert.assertEquals(r1, graphCycleRemover.removeCycle(n1));
    }

    @Test
    public void testDisjointCycles() {
        Node n1 = new Node("1");
        Node n2 = new Node("2");
        Node n3 = new Node("3");
        Node n4 = new Node("4");
        n1.setNexts(Collections.singletonList(n2));
        n2.setNexts(Arrays.asList(n1, n3));
        n3.setNexts(Collections.singletonList(n4));
        n4.setNexts(Collections.singletonList(n3));

        Node r1a2 = new Node(Arrays.asList("1", "2"));
        Node r3a4 = new Node(Arrays.asList("3", "4"));
        r1a2.setNexts(Collections.singletonList(r3a4));

        Assert.assertEquals(r1a2, graphCycleRemover.removeCycle(n1));
    }


    @Test
    public void testCycleIntersection() {
        Node n1 = new Node("1");
        Node n2 = new Node("2");
        Node n3 = new Node("3");
        Node n4 = new Node("4");
        n1.setNexts(Collections.singletonList(n2));
        n2.setNexts(Collections.singletonList(n3));
        n3.setNexts(Arrays.asList(n2, n4));
        n4.setNexts(Collections.singletonList(n3));

        Node r1 = new Node("1");
        Node r2a3a4 = new Node(Arrays.asList("2", "3", "4"));
        r1.setNexts(Collections.singletonList(r2a3a4));

        Assert.assertEquals(r1, graphCycleRemover.removeCycle(n1));
    }

    @Test
    public void testBiggerCycle() {
        Node n1 = new Node("1");
        Node n2 = new Node("2");
        Node n3 = new Node("3");
        Node n4 = new Node("4");
        n1.setNexts(Collections.singletonList(n2));
        n2.setNexts(Collections.singletonList(n3));
        n3.setNexts(Arrays.asList(n2, n4));
        n4.setNexts(Collections.singletonList(n1));

        Node r1a2a3a4 = new Node(Arrays.asList("1", "2", "3", "4"));

        Assert.assertEquals(r1a2a3a4, graphCycleRemover.removeCycle(n1));
    }

    @Test
    public void testSmallerCycle() {
        Node n1 = new Node("1");
        Node n2 = new Node("2");
        Node n3 = new Node("3");
        Node n4 = new Node("4");
        Node n5 = new Node("5");
        n1.setNexts(Collections.singletonList(n2));
        n2.setNexts(Arrays.asList(n3, n4));
        n3.setNexts(Collections.singletonList(n1));
        n4.setNexts(Collections.singletonList(n5));
        n5.setNexts(Collections.singletonList(n2));

        Node r1a2a3a4a5 = new Node(Arrays.asList("1", "2", "3", "4", "5"));

        Assert.assertEquals(r1a2a3a4a5, graphCycleRemover.removeCycle(n1));
    }

    @Test
    public void testEdgedCycle() {
        Node n1 = new Node("1");
        Node n2 = new Node("2");
        Node n3 = new Node("3");
        Node n4 = new Node("4");
        n1.setNexts(Collections.singletonList(n2));
        n2.setNexts(Arrays.asList(n3, n4));
        n3.setNexts(Collections.singletonList(n2));
        n4.setNexts(Collections.singletonList(n2));

        Node r1 = new Node("1");
        Node r2a3a4 = new Node(Arrays.asList("2", "3", "4"));
        r1.setNexts(Collections.singletonList(r2a3a4));

        Assert.assertEquals(r1, graphCycleRemover.removeCycle(n1));
    }


    @Test
    public void testCycleWithSameNext() {
        Node n1 = new Node("1");
        Node n2 = new Node("2");
        Node n3 = new Node("3");
        Node n4 = new Node("4");
        Node n5 = new Node("5");
        n1.setNexts(Collections.singletonList(n2));
        n2.setNexts(Collections.singletonList(n3));
        n3.setNexts(Arrays.asList(n2, n4, n5));
        n5.setNexts(Arrays.asList(n3, n4));

        Node r1 = new Node("1");
        Node r2a3a5 = new Node(Arrays.asList("2", "3", "5"));
        Node r4 = new Node("4");
        r1.setNexts(Collections.singletonList(r2a3a5));
        r2a3a5.setNexts(Collections.singletonList(r4));

        Assert.assertEquals(r1, graphCycleRemover.removeCycle(n1));
    }

}
