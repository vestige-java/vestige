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

package fr.gaellalire.vestige.system;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;

/**
 * @author Gael Lalire
 */
public final class ParseUtil {

    private ParseUtil() {
    }

    public static String decode(final String s) {
        int n = s.length();
        if ((n == 0) || (s.indexOf('%') < 0)) {
            return s;
        }

        StringBuilder sb = new StringBuilder(n);
        ByteBuffer bb = ByteBuffer.allocate(n);
        CharBuffer cb = CharBuffer.allocate(n);
        CharsetDecoder dec = Charset.forName("UTF-8").newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT);

        char c = s.charAt(0);
        for (int i = 0; i < n;) {
            if (c != '%') {
                sb.append(c);
                if (++i >= n) {
                    break;
                }
                c = s.charAt(i);
                continue;
            }
            bb.clear();
            for (;;) {
                try {
                    bb.put(unescape(s, i));
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException();
                }
                i += 3;
                if (i >= n) {
                    break;
                }
                c = s.charAt(i);
                if (c != '%') {
                    break;
                }
            }
            bb.flip();
            cb.clear();
            dec.reset();
            CoderResult cr = dec.decode(bb, cb, true);
            if (cr.isError()) {
                throw new IllegalArgumentException("Error decoding percent encoded characters");
            }
            cr = dec.flush(cb);
            if (cr.isError()) {
                throw new IllegalArgumentException("Error decoding percent encoded characters");
            }
            sb.append(cb.flip().toString());
        }

        return sb.toString();
    }

    private static byte unescape(final String s, final int i) {
        return (byte) Integer.parseInt(s.substring(i + 1, i + 3), 16);
    }

}
