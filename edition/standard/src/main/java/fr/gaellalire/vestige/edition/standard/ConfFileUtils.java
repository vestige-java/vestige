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

package fr.gaellalire.vestige.edition.standard;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;

/**
 * @author Gael Lalire
 */
public final class ConfFileUtils {

    private ConfFileUtils() {
    }

    public static void copy(final InputStream inputStream, final File destFile) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        try {
            PrintWriter printWriter = new PrintWriter(destFile);
            try {
                String line = bufferedReader.readLine();
                while (line != null) {
                    printWriter.println(line);
                    line = bufferedReader.readLine();
                }
            } finally {
                printWriter.close();
            }
        } finally {
            bufferedReader.close();
        }
    }

}
