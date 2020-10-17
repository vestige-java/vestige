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

package fr.gaellalire.vestige.utils;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Gael Lalire
 * @param <E> property type
 */
public abstract class Property<E> implements Serializable {

    private static final long serialVersionUID = 3213799486269632767L;

    private transient E value;

    private String rawValue;

    private Map<String, String> expandMap;

    public Property(final String rawValue) {
        this.rawValue = rawValue;
        if (rawValue != null) {
            String expanded = expand(rawValue);
            if (expanded != null) {
                value = convert(expanded);
            }
        }
    }

    private void readObject(final java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        if (rawValue != null) {
            String expanded = expand(rawValue);
            if (expanded != null) {
                value = convert(expanded);
            }
        }
    }

    public abstract E convert(String value);

    private String expand(final String value) throws ExpandException {

        int p = value.indexOf("${", 0);

        // no special characters
        if (p == -1) {
            return value;
        }

        StringBuffer sb = new StringBuffer(value.length());
        int max = value.length();
        int i = 0; // index of last character we copied

        scanner: while (p < max) {
            if (p > i) {
                // copy in anything before the special stuff
                sb.append(value.substring(i, p));
                i = p;
            }
            int pe = p + 2;

            // do not expand ${{ ... }}
            if (pe < max && value.charAt(pe) == '{') {
                pe = value.indexOf("}}", pe);
                if (pe == -1 || pe + 2 == max) {
                    // append remaining chars
                    sb.append(value.substring(p));
                    break scanner;
                } else {
                    // append as normal text
                    pe++;
                    sb.append(value.substring(p, pe + 1));
                }
            } else {
                String dval = null;
                char lv = value.charAt(pe);
                while ((pe < max) && (lv != '}') && lv != ':') {
                    pe++;
                    lv = value.charAt(pe);
                }
                if (pe == max) {
                    // no matching '}' found, just add in as normal text
                    sb.append(value.substring(p, pe));
                    break scanner;
                }
                String prop = value.substring(p + 2, pe);
                if (lv == ':') {
                    pe++;
                    lv = value.charAt(pe);
                    if (lv == '-') {
                        int spe = pe + 1;
                        do {
                            pe++;
                            lv = value.charAt(pe);
                        } while ((pe < max) && (lv != '}'));
                        dval = value.substring(spe, pe);
                    }
                }
                if (prop.equals("/")) {
                    sb.append(File.separatorChar);
                } else {
                    if (expandMap == null) {
                        expandMap = new HashMap<String, String>();
                    }
                    String val = System.getProperty(prop);
                    if (val == null) {
                        val = System.getenv(prop);
                        if (val == null) {
                            if (dval != null) {
                                sb.append(dval);
                            } else {
                                throw new ExpandException("unable to expand property " + prop);
                            }
                        }
                    }
                    expandMap.put(prop, val);
                    if (val != null) {
                        sb.append(val);
                    }
                }
            }
            i = pe + 1;
            p = value.indexOf("${", i);
            if (p == -1) {
                // no more to expand. copy in any extra
                if (i < max) {
                    sb.append(value.substring(i, max));
                }
                // break out of loop
                break scanner;
            }
        }
        if (sb.length() == 0) {
            return null;
        }
        return sb.toString();
    }

    public E getValue() {
        return value;
    }

    /**
     * @return null if the value is not defined through a property
     */
    public String getRawValue() {
        return rawValue;
    }

    /**
     * @return null if the value is not defined through a property
     */
    public Map<String, String> getExpandMap() {
        return expandMap;
    }

}
