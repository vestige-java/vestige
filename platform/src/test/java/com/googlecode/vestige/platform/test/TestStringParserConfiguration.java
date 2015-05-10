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

package com.googlecode.vestige.platform.test;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.jar.JarInputStream;
import java.util.zip.ZipEntry;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import com.googlecode.vestige.core.parser.StringParser;
import com.googlecode.vestige.platform.AbstractStringParserFactory;
import com.googlecode.vestige.platform.MinimalStringParserFactory;

/**
 * @author Gael Lalire
 */
public class TestStringParserConfiguration {

    // @Test
    // public void test() throws Exception {
    // DefaultStringParserConfigurationFactory
    // defaultStringParserConfigurationFactory = new
    // DefaultStringParserConfigurationFactory();
    // long time;
    // URL[] urls = new URL[1];
    // urls[0] =
    // TestStringParserConfiguration.class.getResource("/commons-io-2.0.jar");
    // TreeMap<String, List<Integer>> pathsByResourceName = new TreeMap<String,
    // List<Integer>>();
    // TreeMap<String, List<Integer>> pathsByClassName = new TreeMap<String,
    // List<Integer>>();
    // map(urls[0], pathsByResourceName, pathsByClassName);
    // System.out.println("NbResources : " + pathsByResourceName.size() +
    // ", NbClasses : " + pathsByClassName.size());
    //
    // time = System.currentTimeMillis();
    // ClassLoaderConfiguration clc = new
    // ClassLoaderConfiguration((Serializable) Arrays.asList(urls), urls,
    // Collections.<ClassLoaderConfiguration>emptyList(), null,
    // defaultStringParserConfigurationFactory.createStringParserConfiguration(pathsByClassName,
    // EMPTY_PATHS),
    // defaultStringParserConfigurationFactory.createStringParserConfiguration(pathsByResourceName,
    // EMPTY_PATHS));
    // System.out.println("Ctor time : " + (System.currentTimeMillis() - time));
    //
    // ByteArrayOutputStream byteArrayOutputStream = new
    // ByteArrayOutputStream();
    // ObjectOutputStream objectOutputStream = new
    // ObjectOutputStream(byteArrayOutputStream);
    // time = System.currentTimeMillis();
    // objectOutputStream.writeObject(clc);
    // objectOutputStream.close();
    // System.out.println("Ser time : " + (System.currentTimeMillis() - time));
    //
    // time = System.currentTimeMillis();
    // ObjectInputStream objectInputStream = new ObjectInputStream(new
    // ByteArrayInputStream(byteArrayOutputStream.toByteArray()));
    // Object readObject = objectInputStream.readObject();
    // System.out.println("Unser time : " + (System.currentTimeMillis() -
    // time));
    // }

    @Test
    public void test() {
        Map<String, Integer> map = new HashMap<String, Integer>();
        map.put("META-INF/LICENSE.txt", 42);
        map.put("META-INF/NOTICE.txt", 43);
        map.put("META-INF/maven/", 1);
        map.put("META-INF/maven/com.googlecode.noweco/", 0);
        map.put("META-INF/maven/com.googlecode.noweco/noweco.modules.installer/", 0);
        map.put("META-INF/maven/com.googlecode.noweco/noweco.modules.installer/pom.properties", 0);
        map.put("META-INF/maven/com.googlecode.noweco/noweco.modules.installer/pom.xml", 0);
        map.put("META-INF/maven/commons-io/", 44);
        map.put("META-INF/maven/commons-io/commons-io/", 45);
        map.put("META-INF/maven/commons-io/commons-io/pom.properties", 46);
        map.put("META-INF/maven/commons-io/commons-io/pom.xml", 47);
        map.put("META-INF/maven/org.slf4j/", 2);
        map.put("META-INF/maven/org.slf4j/slf4j-api/", 3);
        map.put("META-INF/maven/org.slf4j/slf4j-api/pom.properties", 4);
        map.put("META-INF/maven/org.slf4j/slf4j-api/pom.xml", 5);
        map.put("META-INF/maven/org.slf4j/slf4j-jdk14/", 6);
        map.put("META-INF/maven/org.slf4j/slf4j-jdk14/pom.properties", 7);
        map.put("META-INF/maven/org.slf4j/slf4j-jdk14/pom.xml", 8);
        map.put("com/", 660);
        map.put("com/googlecode/", 0);
        map.put("com/googlecode/noweco/", 0);
        map.put("com/googlecode/noweco/installer/", 0);
        map.put("com/googlecode/noweco/installer/NowecoInstaller.class", 0);
        map.put("com/googlecode/noweco/installer/settings.xml", 0);
        map.put("org/", 9);
        map.put("org/apache/", 48);
        map.put("org/apache/commons/", 49);
        map.put("org/apache/commons/io/", 50);
        map.put("org/apache/commons/io/ByteOrderMark.class", 51);
        map.put("org/apache/commons/io/CopyUtils.class", 52);
        map.put("org/apache/commons/io/DirectoryWalker$CancelException.class", 53);
        map.put("org/apache/commons/io/DirectoryWalker.class", 54);
        map.put("org/apache/commons/io/EndianUtils.class", 55);
        map.put("org/apache/commons/io/FileCleaner.class", 56);
        map.put("org/apache/commons/io/FileCleaningTracker$Reaper.class", 57);
        map.put("org/apache/commons/io/FileCleaningTracker$Tracker.class", 58);
        map.put("org/apache/commons/io/FileCleaningTracker.class", 59);
        map.put("org/apache/commons/io/FileDeleteStrategy$ForceFileDeleteStrategy.class", 60);
        map.put("org/apache/commons/io/FileDeleteStrategy.class", 61);
        map.put("org/apache/commons/io/FileExistsException.class", 62);
        map.put("org/apache/commons/io/FileSystemUtils.class", 63);
        map.put("org/apache/commons/io/FileUtils.class", 64);
        map.put("org/apache/commons/io/FilenameUtils.class", 65);
        map.put("org/apache/commons/io/HexDump.class", 66);
        map.put("org/apache/commons/io/IOCase.class", 67);
        map.put("org/apache/commons/io/IOExceptionWithCause.class", 68);
        map.put("org/apache/commons/io/IOUtils.class", 69);
        map.put("org/apache/commons/io/LineIterator.class", 70);
        map.put("org/apache/commons/io/TaggedIOException.class", 71);
        map.put("org/apache/commons/io/ThreadMonitor.class", 72);
        map.put("org/apache/commons/io/comparator/", 73);
        map.put("org/apache/commons/io/comparator/AbstractFileComparator.class", 74);
        map.put("org/apache/commons/io/comparator/CompositeFileComparator.class", 75);
        map.put("org/apache/commons/io/comparator/DefaultFileComparator.class", 76);
        map.put("org/apache/commons/io/comparator/DirectoryFileComparator.class", 77);
        map.put("org/apache/commons/io/comparator/ExtensionFileComparator.class", 78);
        map.put("org/apache/commons/io/comparator/LastModifiedFileComparator.class", 79);
        map.put("org/apache/commons/io/comparator/NameFileComparator.class", 80);
        map.put("org/apache/commons/io/comparator/PathFileComparator.class", 81);
        map.put("org/apache/commons/io/comparator/ReverseComparator.class", 82);
        map.put("org/apache/commons/io/comparator/SizeFileComparator.class", 83);
        map.put("org/apache/commons/io/filefilter/", 84);
        map.put("org/apache/commons/io/filefilter/AbstractFileFilter.class", 85);
        map.put("org/apache/commons/io/filefilter/AgeFileFilter.class", 86);
        map.put("org/apache/commons/io/filefilter/AndFileFilter.class", 87);
        map.put("org/apache/commons/io/filefilter/CanReadFileFilter.class", 88);
        map.put("org/apache/commons/io/filefilter/CanWriteFileFilter.class", 89);
        map.put("org/apache/commons/io/filefilter/ConditionalFileFilter.class", 90);
        map.put("org/apache/commons/io/filefilter/DelegateFileFilter.class", 91);
        map.put("org/apache/commons/io/filefilter/DirectoryFileFilter.class", 92);
        map.put("org/apache/commons/io/filefilter/EmptyFileFilter.class", 93);
        map.put("org/apache/commons/io/filefilter/FalseFileFilter.class", 94);
        map.put("org/apache/commons/io/filefilter/FileFileFilter.class", 95);
        map.put("org/apache/commons/io/filefilter/FileFilterUtils.class", 96);
        map.put("org/apache/commons/io/filefilter/HiddenFileFilter.class", 97);
        map.put("org/apache/commons/io/filefilter/IOFileFilter.class", 98);
        map.put("org/apache/commons/io/filefilter/MagicNumberFileFilter.class", 99);
        map.put("org/apache/commons/io/filefilter/NameFileFilter.class", 100);
        map.put("org/apache/commons/io/filefilter/NotFileFilter.class", 101);
        map.put("org/apache/commons/io/filefilter/OrFileFilter.class", 102);
        map.put("org/apache/commons/io/filefilter/PrefixFileFilter.class", 103);
        map.put("org/apache/commons/io/filefilter/RegexFileFilter.class", 104);
        map.put("org/apache/commons/io/filefilter/SizeFileFilter.class", 105);
        map.put("org/apache/commons/io/filefilter/SuffixFileFilter.class", 106);
        map.put("org/apache/commons/io/filefilter/TrueFileFilter.class", 107);
        map.put("org/apache/commons/io/filefilter/WildcardFileFilter.class", 108);
        map.put("org/apache/commons/io/filefilter/WildcardFilter.class", 109);
        map.put("org/apache/commons/io/input/", 110);
        map.put("org/apache/commons/io/input/AutoCloseInputStream.class", 111);
        map.put("org/apache/commons/io/input/BOMInputStream.class", 112);
        map.put("org/apache/commons/io/input/BoundedInputStream.class", 113);
        map.put("org/apache/commons/io/input/BrokenInputStream.class", 114);
        map.put("org/apache/commons/io/input/CharSequenceReader.class", 115);
        map.put("org/apache/commons/io/input/ClassLoaderObjectInputStream.class", 116);
        map.put("org/apache/commons/io/input/CloseShieldInputStream.class", 117);
        map.put("org/apache/commons/io/input/ClosedInputStream.class", 118);
        map.put("org/apache/commons/io/input/CountingInputStream.class", 119);
        map.put("org/apache/commons/io/input/DemuxInputStream.class", 120);
        map.put("org/apache/commons/io/input/NullInputStream.class", 121);
        map.put("org/apache/commons/io/input/NullReader.class", 122);
        map.put("org/apache/commons/io/input/ProxyInputStream.class", 123);
        map.put("org/apache/commons/io/input/ProxyReader.class", 124);
        map.put("org/apache/commons/io/input/ReaderInputStream.class", 125);
        map.put("org/apache/commons/io/input/SwappedDataInputStream.class", 126);
        map.put("org/apache/commons/io/input/TaggedInputStream.class", 127);
        map.put("org/apache/commons/io/input/Tailer.class", 128);
        map.put("org/apache/commons/io/input/TailerListener.class", 129);
        map.put("org/apache/commons/io/input/TailerListenerAdapter.class", 130);
        map.put("org/apache/commons/io/input/TeeInputStream.class", 131);
        map.put("org/apache/commons/io/input/XmlStreamReader.class", 132);
        map.put("org/apache/commons/io/input/XmlStreamReaderException.class", 133);
        map.put("org/apache/commons/io/monitor/", 134);
        map.put("org/apache/commons/io/monitor/FileAlterationListener.class", 135);
        map.put("org/apache/commons/io/monitor/FileAlterationListenerAdaptor.class", 136);
        map.put("org/apache/commons/io/monitor/FileAlterationMonitor.class", 137);
        map.put("org/apache/commons/io/monitor/FileAlterationObserver.class", 138);
        map.put("org/apache/commons/io/monitor/FileEntry.class", 139);
        map.put("org/apache/commons/io/output/", 140);
        map.put("org/apache/commons/io/output/BrokenOutputStream.class", 141);
        map.put("org/apache/commons/io/output/ByteArrayOutputStream.class", 142);
        map.put("org/apache/commons/io/output/CloseShieldOutputStream.class", 143);
        map.put("org/apache/commons/io/output/ClosedOutputStream.class", 144);
        map.put("org/apache/commons/io/output/CountingOutputStream.class", 145);
        map.put("org/apache/commons/io/output/DeferredFileOutputStream.class", 146);
        map.put("org/apache/commons/io/output/DemuxOutputStream.class", 147);
        map.put("org/apache/commons/io/output/FileWriterWithEncoding.class", 148);
        map.put("org/apache/commons/io/output/LockableFileWriter.class", 149);
        map.put("org/apache/commons/io/output/NullOutputStream.class", 150);
        map.put("org/apache/commons/io/output/NullWriter.class", 151);
        map.put("org/apache/commons/io/output/ProxyOutputStream.class", 152);
        map.put("org/apache/commons/io/output/ProxyWriter.class", 153);
        map.put("org/apache/commons/io/output/StringBuilderWriter.class", 154);
        map.put("org/apache/commons/io/output/TaggedOutputStream.class", 155);
        map.put("org/apache/commons/io/output/TeeOutputStream.class", 156);
        map.put("org/apache/commons/io/output/ThresholdingOutputStream.class", 157);
        map.put("org/apache/commons/io/output/WriterOutputStream.class", 158);
        map.put("org/apache/commons/io/output/XmlStreamWriter.class", 159);
        map.put("org/slf4j/", 10);
        map.put("org/slf4j/ILoggerFactory.class", 11);
        map.put("org/slf4j/IMarkerFactory.class", 12);
        map.put("org/slf4j/Logger.class", 13);
        map.put("org/slf4j/LoggerFactory.class", 14);
        map.put("org/slf4j/MDC.class", 15);
        map.put("org/slf4j/Marker.class", 16);
        map.put("org/slf4j/MarkerFactory.class", 17);
        map.put("org/slf4j/helpers/", 18);
        map.put("org/slf4j/helpers/BasicMDCAdapter.class", 19);
        map.put("org/slf4j/helpers/BasicMarker.class", 20);
        map.put("org/slf4j/helpers/BasicMarkerFactory.class", 21);
        map.put("org/slf4j/helpers/FormattingTuple.class", 22);
        map.put("org/slf4j/helpers/MarkerIgnoringBase.class", 23);
        map.put("org/slf4j/helpers/MessageFormatter.class", 24);
        map.put("org/slf4j/helpers/NOPLogger.class", 25);
        map.put("org/slf4j/helpers/NOPLoggerFactory.class", 26);
        map.put("org/slf4j/helpers/NOPMDCAdapter.class", 27);
        map.put("org/slf4j/helpers/NamedLoggerBase.class", 28);
        map.put("org/slf4j/helpers/SubstituteLoggerFactory.class", 29);
        map.put("org/slf4j/helpers/Util.class", 30);
        map.put("org/slf4j/impl/", 31);
        map.put("org/slf4j/impl/JDK14LoggerAdapter.class", 32);
        map.put("org/slf4j/impl/JDK14LoggerFactory.class", 33);
        map.put("org/slf4j/impl/StaticLoggerBinder.class", 34);
        map.put("org/slf4j/impl/StaticMDCBinder.class", 35);
        map.put("org/slf4j/impl/StaticMarkerBinder.class", 36);
        map.put("org/slf4j/spi/", 37);
        map.put("org/slf4j/spi/LocationAwareLogger.class", 38);
        map.put("org/slf4j/spi/LoggerFactoryBinder.class", 39);
        map.put("org/slf4j/spi/MDCAdapter.class", 40);
        map.put("org/slf4j/spi/MarkerFactoryBinder.class", 41);

        AbstractStringParserFactory fastStringParserFactory = new MinimalStringParserFactory();
        StringParser createFastStringParser = fastStringParserFactory.createStringParser(map, -1);
        Set<Entry<String, Integer>> entrySet = map.entrySet();
        for (Entry<String, Integer> entry : entrySet) {
            int match = createFastStringParser.match(entry.getKey());
            if (entry.getValue() != match) {
                System.err.println("Fail for " + entry.getKey() + " expected " + entry.getValue() + " got " + match);
                Assert.fail();
            }
        }
    }

    @Test
    public void testMinimal() throws Exception {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(TestStringParserConfiguration.class.getResourceAsStream("/minimal.txt")));
        AbstractStringParserFactory fastStringParserFactory = new MinimalStringParserFactory();
        String l;
        while ((l = bufferedReader.readLine()) != null) {
            Map<String, Integer> map = new TreeMap<String, Integer>();
            l = l.substring(1, l.length() - 1);
            String[] split = l.split(", ");
            for (String kv : split) {
                int indexOf = kv.indexOf('=');
                map.put(kv.substring(0, indexOf), Integer.valueOf(kv.substring(indexOf + 1, kv.length())));
            }
            StringParser createFastStringParser = fastStringParserFactory.createStringParser(map, -1);
            Set<Entry<String, Integer>> entrySet = map.entrySet();
            for (Entry<String, Integer> entry : entrySet) {
                int match = createFastStringParser.match(entry.getKey());
                if (entry.getValue() != match) {
                    System.err.println("Fail for " + entry.getKey() + " expected " + entry.getValue() + " got " + match);
                    Assert.fail();
                }
            }
        }
    }

    @Ignore
    @Test
    public void testFast() throws Exception {
        AbstractStringParserFactory fastStringParserFactory = new MinimalStringParserFactory();
        long time, ttime;
        URL[] urls = new URL[1];
        urls[0] = TestStringParserConfiguration.class.getResource("/commons-io-2.0.jar");
        TreeMap<String, Integer> pathsByResourceName = new TreeMap<String, Integer>();
        map(urls[0], pathsByResourceName);
        System.out.println("NbResources : " + pathsByResourceName.size());

        time = System.currentTimeMillis();
        // ClassLoaderConfiguration clc = new
        // ClassLoaderConfiguration((Serializable) Arrays.asList(urls), urls,
        // Collections.<ClassLoaderConfiguration>emptyList(), null,
        // defaultStringParserConfigurationFactory.createStringParserConfiguration(pathsByClassName,
        // EMPTY_PATHS),
        // defaultStringParserConfigurationFactory.createStringParserConfiguration(pathsByResourceName,
        // EMPTY_PATHS));
        StringParser clc = fastStringParserFactory.createStringParser(pathsByResourceName, -1);
        Integer beforeMatch = pathsByResourceName.get("org/apache/commons/io/CopyUtils.class");
        int match = clc.match("org/apache/commons/io/CopyUtils.class");
        System.out.println(match + " " + beforeMatch);

        System.out.println("Ctor time : " + (System.currentTimeMillis() - time));

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
        time = System.currentTimeMillis();
        objectOutputStream.writeObject(clc);
        objectOutputStream.close();
        ttime = (System.currentTimeMillis() - time);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        System.out.println("Fast Ser time : " + ttime + " size " + byteArray.length);

        time = System.currentTimeMillis();
        ObjectInputStream objectInputStream = new ObjectInputStream(new ByteArrayInputStream(byteArray));
        @SuppressWarnings("unused")
        Object readObject = objectInputStream.readObject();
        System.out.println("Fast Unser time : " + (System.currentTimeMillis() - time));
    }

    @Test
    @Ignore
    public void testMap() throws Exception {
        long time, ttime;
        URL[] urls = new URL[1];
        urls[0] = TestStringParserConfiguration.class.getResource("/commons-io-2.0.jar");
        TreeMap<String, Integer> pathsByResourceName = new TreeMap<String, Integer>();
        map(urls[0], pathsByResourceName);

        Object clc = pathsByResourceName;

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
        time = System.currentTimeMillis();
        objectOutputStream.writeObject(clc);
        objectOutputStream.close();
        ttime = (System.currentTimeMillis() - time);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        System.out.println("Map Ser time : " + ttime + " size " + byteArray.length);
        time = System.currentTimeMillis();
        ObjectInputStream objectInputStream = new ObjectInputStream(new ByteArrayInputStream(byteArray));
        @SuppressWarnings("unused")
        Object readObject = objectInputStream.readObject();
        System.out.println("Map Unser time : " + (System.currentTimeMillis() - time));
    }

    private void map(final URL url, final TreeMap<String, Integer> pathsByResourceName) {
        try {
            JarInputStream openStream = new JarInputStream(url.openStream());
            ZipEntry nextEntry = openStream.getNextEntry();
            while (nextEntry != null) {
                String name = nextEntry.getName();
                pathsByResourceName.put(name, 1);
                nextEntry = openStream.getNextEntry();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
