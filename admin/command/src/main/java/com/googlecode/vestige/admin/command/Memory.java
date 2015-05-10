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

package com.googlecode.vestige.admin.command;

import java.io.PrintWriter;
import java.util.Collections;
import java.util.List;

import com.googlecode.vestige.admin.command.argument.Argument;

/**
 * @author Gael Lalire
 */
public class Memory implements Command {

    public static String memory(final long il) {
        float l = il;
        if (l < 1024) {
            return String.format("%.0f B", l);
        }
        l = l / 1024;
        if (l < 1024) {
            return String.format("%.2f kB", l);
        }
        l = l / 1024;
        return String.format("%.2f MB", l);
    }

    public String getName() {
        return "memory";
    }

    public List<Argument> getArguments() {
        return Collections.emptyList();
    }

    public String getDesc() {
        return "Show used and free memory";
    }

    public void execute(final PrintWriter out) {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        out.println(memory(totalMemory - freeMemory) + " / " + memory(totalMemory) + " (max "
                + memory(runtime.maxMemory()) + ")");
    }

}
