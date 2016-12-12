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

package fr.gaellalire.vestige.admin.command.argument;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;

import org.junit.Test;

import fr.gaellalire.vestige.admin.command.CommandLineParser;

/**
 * @author Gael Lalire
 */
public class TestCommandLineParser {

    private CommandLineParser commandLineParser;

    private PrintWriter out;

    void check(final String commandLine, final String... expecteds) {
        out.println("echo " + commandLine);
        commandLineParser.setCommandLine(commandLine);
        boolean first = true;
        for (String expected : expecteds) {
            if (first) {
                first = false;
            } else {
                out.print(" ");
            }
            if (!commandLineParser.nextArgument(true)) {
                out.flush();
                throw new RuntimeException("expected " + expected + " got nothing");
            }
            String unescapedValue = commandLineParser.getUnescapedValue();
            if (!expected.equals(unescapedValue)) {
                out.println(unescapedValue);
                out.flush();
                throw new RuntimeException("expected " + expected + " got " + unescapedValue);
            }
            out.print(expected);
        }
        out.println();
        out.println();
        if (commandLineParser.nextArgument(true)) {
            throw new RuntimeException("expected nothing got " + commandLineParser.getUnescapedValue());
        }
    }

    @Test
    public void testCommandLine() {
        out = new PrintWriter(new Writer() {

            @Override
            public void write(final char[] cbuf, final int off, final int len) throws IOException {
            }

            @Override
            public void flush() throws IOException {
            }

            @Override
            public void close() throws IOException {
            }
        });
        // out = new PrintWriter(System.out, true);
        commandLineParser = new CommandLineParser();

        check("abc", "abc");
        check("abc ", "abc", "");
        check("  abc", "abc");
        check("  'abc k'", "abc k");
        check("  'abc\\ k'", "abc\\ k");
        check("  'abc\\k'", "abc\\k");
        check("  'abc\\\\k'", "abc\\\\k");
        check("  \"abc\\ k\"", "abc\\ k");
        check("  \"abc\\k\"", "abc\\k");
        check("  \"abc\\\\k\"", "abc\\k");
        check("  abc\\ k", "abc k");
        check("  abc\\k", "abck");
        check("  abc\\\\k", "abc\\k");
        check("  abc  k", "abc", "k");
        check("  \"abc\\\"  k\"", "abc\"  k");
        check("  'abc\\'  k''", "abc\\", "k");
        check("  'abc'\\''  k'", "abc'  k");
        check("  'abc'\\'klo'  k'", "abc'klo  k"); //
        // check("  \"abc\\", "abc\\");
        check("file /Informations\\ sur\\ l’utilisateur/", "file", "/Informations sur l’utilisateur/");
        check("file \\\\k", "file", "\\k");
        check("file \\\\\\\\k", "file", "\\\\k");

        check("uninstall jk\\\'r\\\" ", "uninstall", "jk'r\"", "");

        // commandLineParser.setCommandLine("ab\\ cd");
        // commandLineParser.nextArgument();
        // System.out.println(commandLineParser.getEscapePosition(6));

        out.println("ALL OK");

    }

}
