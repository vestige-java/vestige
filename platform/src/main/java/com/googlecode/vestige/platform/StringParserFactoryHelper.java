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

package com.googlecode.vestige.platform;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * @author Gael Lalire
 */
public class StringParserFactoryHelper {

    private int maxDataState = 0;

    private TreeSet<Character> characters = new TreeSet<Character>();

    private List<TreeMap<Character, Integer>> states = new ArrayList<TreeMap<Character, Integer>>();

    private List<Integer> data = new ArrayList<Integer>();

    private List<Integer> dataState = new ArrayList<Integer>();

    private Integer oneStateValue;

    public StringParserFactoryHelper() {
        states.add(new TreeMap<Character, Integer>());
    }

    public void addName(final String name, final boolean complete, final int value) {
//        System.out.println("Add " + name + " " + complete + " (" + value + ")");
        int state = 1;
        TreeMap<Character, Integer> integerByState = null;
        char c = 0;
        Integer integer = null;
        for (int i = 0; i < name.length(); i++) {
            c = name.charAt(i);
            characters.add(c);
            integerByState = states.get(state - 1);
            integer = integerByState.get(c);
            if (integer == null) {
                integer = states.size() + 1;
                states.add(new TreeMap<Character, Integer>());
                integerByState.put(c, integer);
            }
            state = integer.intValue();
        }
        if (!complete) {
            if (integerByState != null) {
                integerByState.put(c, -state);
            } else {
                oneStateValue = value;
            }
        }

        // keep dataState in asc order
        if (state >= maxDataState) {
            data.add(value);
            dataState.add(state);
            maxDataState = state;
        } else {
            int i = dataState.size() - 1;
            while (i >= 0 && dataState.get(i) > state) {
                i--;
            }
            dataState.add(i + 1, state);
            data.add(i + 1, value);
        }
    }

    public TreeSet<Character> getCharacters() {
        return characters;
    }

    public Integer getOneStateValue() {
        return oneStateValue;
    }

    public List<Integer> getData() {
        return data;
    }

    public List<Integer> getDataState() {
        return dataState;
    }

    public List<TreeMap<Character, Integer>> getStates() {
        return states;
    }

}
