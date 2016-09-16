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

package fr.gaellalire.vestige.admin.ssh;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * @author Gael Lalire
 */
public class LineFilterOutputStream extends FilterOutputStream {

    private boolean crReaded;

    public LineFilterOutputStream(final OutputStream out) {
        super(out);
    }

    @Override
    public void write(final int b) throws IOException {
        if (!crReaded && b == '\n') {
            out.write('\r');
            out.write('\n');
        } else if (b != '\n') {
            out.write(b);
        }
        if (b == '\r') {
            crReaded = true;
            out.write('\n');
        } else {
            crReaded = false;
        }
    }

    public void clear(final int size) throws IOException {
        out.write('\r');
        for (int i = 0; i < size; i++) {
            out.write(' ');
        }
        out.write('\r');
        out.flush();
    }

}
