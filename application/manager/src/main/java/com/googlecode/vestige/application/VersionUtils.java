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

package com.googlecode.vestige.application;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Gael Lalire
 */
public final class VersionUtils {

    public static final Pattern VERSION_PATTERN = Pattern.compile("(\\d+)\\.(\\d+)\\.(\\d+)");

    /**
     *
     * @author Gael Lalire
     */
    private static class VersionComparator implements Comparator<List<Integer>>, Serializable {

        private static final long serialVersionUID = -4113755399515029010L;

        public int compare(final List<Integer> o1, final List<Integer> o2) {
            int delta = o1.size() - o2.size();
            if (delta != 0) {
                return delta;
            }
            int s = o1.size();
            for (int i = 0; i < s; i++) {
                delta = o1.get(i) - o2.get(i);
                if (delta != 0) {
                    return delta;
                }
            }
            return 0;
        }
    }

    public static final Comparator<List<Integer>> VERSION_COMPARATOR = new VersionComparator();

    private VersionUtils() {
    }

    /**
     * @return
     * < 0 if version < otherVersion
     * = 0 if version = otherVersion
     * > 0 if version > otherVersion
     * null if we cannot compare.
     */
    public static Integer compare(final List<Integer> version, final List<Integer> otherVersion) {
        for (int i = 0; i < 3; i++) {
            if (!otherVersion.get(i).equals(version.get(i))) {
                if (otherVersion.get(i).intValue() < version.get(i).intValue()) {
                    // before version
                    for (int j = i + 1; j < 3; j++) {
                        if (otherVersion.get(j).intValue() != 0) {
                            return null;
                        }
                    }
                    return Integer.valueOf(1);
                } else {
                    // after version
                    for (int j = i + 1; j < 3; j++) {
                        if (version.get(j).intValue() != 0) {
                            return null;
                        }
                    }
                    return Integer.valueOf(-1);
                }
            }
        }
        return Integer.valueOf(0);
    }

    public static List<Integer> fromString(final String version) {
        Matcher matcher = VERSION_PATTERN.matcher(version);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Incorrect version : " + version);
        }
        Integer[] res = new Integer[3];
        for (int i = 0; i < res.length; i++) {
            res[i] = Integer.valueOf(matcher.group(i + 1));
        }
        return Arrays.asList(res);
    }

    public static String toString(final List<Integer> version) {
        if (version.size() != 3) {
            throw new IllegalArgumentException("Incorrect version : " + version);
        }
        StringBuilder sb = new StringBuilder(Integer.toString(version.get(0)));
        for (int i = 1; i < version.size(); i++) {
            sb.append('.');
            sb.append(version.get(i));
        }
        return sb.toString();
    }

}
