//package com.googlecode.vestige.platform.test;
//
//import java.net.URL;
//import java.util.Collections;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.TreeMap;
//
//import org.junit.Test;
//
//import com.googlecode.vestige.core.VestigeClassLoader;
//import com.googlecode.vestige.platform.DefaultStringParserConfigurationFactory;
//import com.googlecode.vestige.platform.StringParserConfiguration;
//
///**
// * @author Gael Lalire
// */
//public class TestStringParser {
//
//    @Test
//    public void testMinimal() throws Exception {
//        DefaultStringParserConfigurationFactory stringParserFactory = new DefaultStringParserConfigurationFactory();
//        TreeMap<String, List<VestigeClassLoader>> map = new TreeMap<String, List<VestigeClassLoader>>();
//        VestigeClassLoader v1 = new VestigeClassLoader(null, null, null, new URL[] {});
//        VestigeClassLoader v2 = new VestigeClassLoader(null, null, null, new URL[] {});
////        v2 = v1;
//        System.out.println(v1 + " " + v2);
//        map.put("a", Collections.singletonList(v2));
//        map.put("abcd", Collections.singletonList(v2));
//        map.put("abch", Collections.singletonList(v2));
//        map.put("com", Collections.singletonList(v1));
//        map.put("coma", Collections.singletonList(v1));
//        map.put("comb.d.v", Collections.singletonList(v2));
//        map.put("comb.dk", Collections.singletonList(v1));
//        map.put("d", Collections.singletonList(v1));
//
//        System.out.println(stringParserFactory.createStringParserConfiguration(map, null));
////        StringParser<List<VestigeClassLoader>> createStringParser = stringParserFactory.createStringParser(map);
//
// //       List<VestigeClassLoader> match = createStringParser.match("com.d.v");
// //       match.get(0);
//  //      match = createStringParser.match("com.dk");
//   //     match.get(0);
//    }
//
//
//    @Test
//    public void test() throws Exception {
//        DefaultStringParserConfigurationFactory stringParserFactory = new DefaultStringParserConfigurationFactory();
//        Map<String, List<VestigeClassLoader>> map = new HashMap<String, List<VestigeClassLoader>>();
//        map.put("com.d.v", Collections.singletonList((VestigeClassLoader) null));
//        map.put("com.dk", Collections.singletonList((VestigeClassLoader) null));
//        StringParserConfiguration<List<VestigeClassLoader>> createStringParserConfiguration = stringParserFactory.createStringParserConfiguration(map, null);
////        StringParser<List<VestigeClassLoader>> createStringParser = new StringParser<List<VestigeClassLoader>>(states, data, defaultValue);
////
////        List<VestigeClassLoader> match = createStringParser.match("com.d.v");
////        match.get(0);
////        match = createStringParser.match("com.dk");
////        match.get(0);
//    }
//
//}
