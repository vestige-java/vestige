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

package com.googlecode.vestige.admin.command.argument;

import java.util.Arrays;
import java.util.Collection;

/**
 * @author Gael Lalire
 */
public class LevelArgument implements Argument {

    private static final String NAME = "<level>";

    public String getName() {
        return NAME;
    }

    private int level;

    public LevelArgument() {
    }

    public int getLevel() {
        return level;
    }

    public void parse(final String s) throws ParseException {
        int level;
        try {
            level = Integer.parseInt(s);
        } catch (NumberFormatException e) {
            throw new ParseException(e);
        }
        if (level < 0 || level > 3) {
            throw new ParseException("Invalid level");
        }
        this.level = level;
    }

    public Collection<String> propose() {
        return Arrays.asList("0", "1", "2", "3");
    }

    public void reset() {
        level = -1;
    }

}
