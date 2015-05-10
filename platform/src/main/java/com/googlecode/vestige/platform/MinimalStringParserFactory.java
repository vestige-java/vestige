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
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * @author Gael Lalire
 */
public class MinimalStringParserFactory extends AbstractStringParserFactory {

    public int diffPosition(final String s1, final String s2) {
        int length = Math.min(s1.length(), s2.length());
        for (int i = 0; i < length; i++) {
            if (s1.charAt(i) != s2.charAt(i)) {
                return i;
            }
        }
        return length;
    }

    @Override
    public void createStates(final Map<String, Integer> valueByName, final StringParserFactoryHelper states) {
        TreeMap<String, Integer> treeValueByName;
        if (valueByName instanceof TreeMap) {
            treeValueByName = (TreeMap<String, Integer>) valueByName;
        } else {
            treeValueByName = new TreeMap<String, Integer>(valueByName);
        }

        List<String> starNames = new ArrayList<String>();
        String previousName = null;
        String commonPart = "";
        Integer lastValue = null;
        for (Entry<String, Integer> entry : treeValueByName.entrySet()) {
            String name = entry.getKey();
            Integer value = entry.getValue();
            if (previousName == null) {
                starNames.add(name);
                lastValue = value;
            } else {
                int diffPosition = diffPosition(previousName, name);
                ListIterator<String> listIterator = starNames.listIterator(starNames.size());
                while (listIterator.hasPrevious()) {
                    String previous = listIterator.previous();
                    int commonDiff = Math.max(diffPosition, diffPosition(previous, commonPart));
                    if (previous.length() > commonDiff) {
                        String nprevious = previous.substring(0, commonDiff + 1);
                        if (!nprevious.endsWith("*")) {
                            nprevious = nprevious.concat("*");
                        }
                        listIterator.set(nprevious);
                    }
                }
                if (!lastValue.equals(value)) {
                    for (String starName : new TreeSet<String>(starNames)) {
                        if (starName.endsWith("*")) {
                            states.addName(starName.substring(0, starName.length() - 1), false, lastValue);
                        } else {
                            states.addName(starName, true, lastValue);
                        }
                    }
                    starNames.clear();
                    lastValue = value;
                    commonPart = previousName.substring(0, diffPosition);
                }
                starNames.add(name);
            }
            previousName = name;
        }
        if (states.getData().size() == 0) {
            states.addName("", false, lastValue);
        } else {
            ListIterator<String> listIterator = starNames.listIterator(starNames.size());
            while (listIterator.hasPrevious()) {
                String previous = listIterator.previous();
                int commonDiff = diffPosition(previous, commonPart);
                if (previous.length() > commonDiff) {
                    String nprevious = previous.substring(0, commonDiff + 1);
                    if (!nprevious.endsWith("*")) {
                        nprevious = nprevious.concat("*");
                    }
                    listIterator.set(nprevious);
                }
            }
            for (String starName : new TreeSet<String>(starNames)) {
                if (starName.endsWith("*")) {
                    states.addName(starName.substring(0, starName.length() - 1), false, lastValue);
                } else {
                    states.addName(starName, true, lastValue);
                }
            }
        }
    }

}
