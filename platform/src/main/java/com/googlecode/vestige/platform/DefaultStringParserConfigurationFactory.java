//package com.googlecode.vestige.platform;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Map;
//import java.util.Map.Entry;
//import java.util.TreeMap;
//
//import com.googlecode.vestige.core.parser.ListIntegerByCharacter;
//
///**
// * @author Gael Lalire
// */
//public class DefaultStringParserConfigurationFactory implements StringParserConfigurationFactory {
//
//    public <E> StringParserConfiguration<E> createStringParserConfiguration(
//            final Map<String, E> vestigeClassLoadersByName, final E defaultValue) {
//        List<TreeMap<Character, Integer>> states = new ArrayList<TreeMap<Character, Integer>>();
//        List<E> data = new ArrayList<E>();
//
//        // state 0
//        states.add(new TreeMap<Character, Integer>());
//
//        for (Entry<String, E> entry : vestigeClassLoadersByName.entrySet()) {
//            String name = entry.getKey();
//            int state = 0;
//            for (int i = 0; i < name.length(); i++) {
//                char c = name.charAt(i);
//                TreeMap<Character, Integer> integerByState = states.get(state);
//                Integer integer = integerByState.get(c);
//                if (integer == null) {
//                    integer = states.size();
//                    states.add(new TreeMap<Character, Integer>());
//                    integerByState.put(c, integer);
//                }
//                state = integer.intValue();
//            }
//            int emptyCount = state - data.size();
//            if (emptyCount < 0) {
//                data.set(state, entry.getValue());
//            } else {
//                for (int i = 0; i < emptyCount; i++) {
//                    data.add(null);
//                }
//                data.add(entry.getValue());
//            }
//        }
//
//        List<ListIntegerByCharacter> spStates = new ArrayList<ListIntegerByCharacter>();
//        for (TreeMap<Character, Integer> map : states) {
//            Character firstKey = null;
//            List<Integer> integerByCharacter = new ArrayList<Integer>();
//
//            for (Entry<Character, Integer> entry : map.entrySet()) {
//                Character key = entry.getKey();
//                if (firstKey == null) {
//                    firstKey = key;
//                }
//                int emptyCount = key - firstKey - integerByCharacter.size();
//                for (int i = 0; i < emptyCount; i++) {
//                    integerByCharacter.add(null);
//                }
//                integerByCharacter.add(entry.getValue());
//            }
//            if (firstKey != null) {
//                spStates.add(new ListIntegerByCharacter(firstKey, integerByCharacter));
//            } else {
//                spStates.add(null);
//            }
//        }
//
//        return new StringParserConfiguration<E>(spStates, data, defaultValue);
//    }
//
//}
