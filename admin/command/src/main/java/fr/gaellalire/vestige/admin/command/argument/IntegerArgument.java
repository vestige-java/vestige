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

import java.util.Collection;
import java.util.Collections;

/**
 * @author Gael Lalire
 */
public class IntegerArgument implements Argument {

    private static final String NAME = "<integer>";

    public String getName() {
        return NAME;
    }

    private Integer value;

    public IntegerArgument() {
    }

    public Integer getValue() {
        return value;
    }

    public void parse(final String s) throws ParseException {
        try {
            value = Integer.valueOf(s);
        } catch (NumberFormatException e) {
            throw new ParseException(e);
        }
    }

    public Collection<String> propose() {
        return Collections.emptyList();
    }

    public void reset() {
        value = null;
    }

}
