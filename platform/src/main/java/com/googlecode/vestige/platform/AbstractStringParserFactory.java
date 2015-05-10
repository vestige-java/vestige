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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.TreeSet;

import com.googlecode.vestige.core.parser.IntegerStateStringParser;
import com.googlecode.vestige.core.parser.NoStateStringParser;
import com.googlecode.vestige.core.parser.ShortStateStringParser;
import com.googlecode.vestige.core.parser.StringParser;

/**
 * @author Gael Lalire
 */
public abstract class AbstractStringParserFactory implements StringParserFactory {

    public int[] convertList(final List<Integer> integers) {
        int[] tab = new int[integers.size()];
        int i = 0;
        for (Integer integer : integers) {
            tab[i] = integer;
            i++;
        }
        return tab;
    }

    public abstract void createStates(final Map<String, Integer> valueByName, final StringParserFactoryHelper states);

    public StringParser createStringParser(final Map<String, Integer> valueByName, final int defaultValue) {
        StringParserFactoryHelper stringParserFactoryHelper = new StringParserFactoryHelper();
        createStates(valueByName, stringParserFactoryHelper);

        Integer oneStateValue = stringParserFactoryHelper.getOneStateValue();
        if (oneStateValue != null) {
            return new NoStateStringParser(oneStateValue);
        }

        int size = stringParserFactoryHelper.getData().size();
        int[] data = convertList(stringParserFactoryHelper.getData());
        int[] dataState = convertList(stringParserFactoryHelper.getDataState());
        List<TreeMap<Character, Integer>> states = stringParserFactoryHelper.getStates();
        TreeSet<Character> characters = stringParserFactoryHelper.getCharacters();

        Map<Integer, Integer> switchStates = new HashMap<Integer, Integer>();
        int dataIndex = 0;
        while (dataState[dataIndex] <= size) {
            dataIndex++;
        }
        for (int p = dataIndex - 1; p >= 0; p--) {
            int savData = data[p];
            int ds = dataState[p];
            for (int i = p; i < ds - 1; i++) {
                data[i] = data[i + 1];
            }
            data[ds - 1] = savData;
        }
        int previous = 1;
        for (int intValue : dataState) {
            for (int i = previous; i < intValue && dataIndex < size; i++) {
                // i is not final, dataState[dataIndex] must be final
                switchStates.put(i, dataState[dataIndex]);
                switchStates.put(dataState[dataIndex], i);
                dataIndex++;
            }
            previous = intValue + 1;
        }

        char firstCharacter = characters.first();
        int alphabetSize = characters.last() - firstCharacter + 1;

        short[] characterIds = new short[alphabetSize];
        Arrays.fill(characterIds, (short) -1);
        short idCount = 0;
        for (Character s : characters) {
            characterIds[s - firstCharacter] = idCount;
            idCount++;
        }

        Integer initialState = switchStates.get(1);
        if (initialState == null) {
            initialState = 1;
        }

        int statesNumber = states.size();

        StringParser fastStringParser = null;
        if (statesNumber < Short.MAX_VALUE) {
            short[] stateByCharacterIdAndState = new short[idCount * statesNumber];
            // state 0 is final error state
            Arrays.fill(stateByCharacterIdAndState, (short) 0);
            int state = 1;
            for (TreeMap<Character, Integer> stateConf : states) {
                for (Entry<Character, Integer> entry : stateConf.entrySet()) {
                    short id = characterIds[entry.getKey() - firstCharacter];
                    Integer entryValue = entry.getValue();
                    boolean noExitState = false;
                    if (entryValue < 0) {
                        entryValue = -entryValue;
                        noExitState = true;
                    }
                    Integer destState = switchStates.get(entryValue);
                    if (destState == null) {
                        destState = entryValue;
                    }
                    if (noExitState) {
                        destState = -destState;
                    }
                    Integer fromState = switchStates.get(state);
                    if (fromState == null) {
                        fromState = state;
                    }
                    stateByCharacterIdAndState[id * statesNumber + (fromState.intValue() - 1)] = destState.shortValue();
                }
                state++;
            }
            fastStringParser = new ShortStateStringParser(firstCharacter, characterIds, initialState.shortValue(), stateByCharacterIdAndState, statesNumber, data, defaultValue);
        }  else {
            int[] stateByCharacterIdAndState = new int[idCount * statesNumber];
            Arrays.fill(stateByCharacterIdAndState, -1);
            int state = 1;
            for (TreeMap<Character, Integer> stateConf : states) {
                for (Entry<Character, Integer> entry : stateConf.entrySet()) {
                    short id = characterIds[entry.getKey() - firstCharacter];
                    Integer entryValue = entry.getValue();
                    boolean noExitState = false;
                    if (entryValue < 0) {
                        entryValue = -entryValue;
                        noExitState = true;
                    }
                    Integer destState = switchStates.get(entryValue);
                    if (destState == null) {
                        destState = entryValue;
                    }
                    if (noExitState) {
                        destState = -destState;
                    }
                    Integer fromState = switchStates.get(state);
                    if (fromState == null) {
                        fromState = state;
                    }
                    stateByCharacterIdAndState[id * statesNumber + (fromState.intValue() - 1)] = destState.intValue();
                }
                state++;
            }
            fastStringParser = new IntegerStateStringParser(firstCharacter, characterIds, initialState.intValue(), stateByCharacterIdAndState, statesNumber, data, defaultValue);
        }
        return fastStringParser;
    }


}
