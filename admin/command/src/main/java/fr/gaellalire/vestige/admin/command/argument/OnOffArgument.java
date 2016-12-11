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

import java.util.Arrays;
import java.util.List;

/**
 * @author Gael Lalire
 */
public class OnOffArgument implements Argument {

    private static final String NAME = "<on | off>";

    private static final List<String> ON_OFF = Arrays.asList("on", "off");

    public String getName() {
        return NAME;
    }

    private Boolean active;

    public OnOffArgument() {
    }

    public Boolean getActive() {
        return active;
    }

    public void parse(final String s) throws ParseException {
        if ("on".equals(s)) {
            active = Boolean.TRUE;
        } else if ("off".equals(s)) {
            active = Boolean.FALSE;
        } else {
            throw new ParseException("Expected on or off");
        }
    }

    public void propose(final ProposeContext proposeContext) {
        for (String proposition : ON_OFF) {
            proposeContext.addProposition(proposition);
        }
    }

    public void reset() {
        active = null;
    }

}
