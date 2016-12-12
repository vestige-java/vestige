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

import java.util.Set;
import java.util.TreeSet;

/**
 * @author Gael Lalire
 */
public class SimpleProposeContext implements ProposeContext {

    private Set<String> propositions = new TreeSet<String>();

    public void reset() {
        propositions.clear();
    }

    @Override
    public String getPrefix() {
        return "";
    }

    @Override
    public void setUnescapePrefixHiddenLength(final int length) {
    }

    @Override
    public void addProposition(final String proposition) {
        propositions.add(proposition);
    }

    @Override
    public void addUnterminatedProposition(final String proposition) {
        propositions.add(proposition);
    }

    public Set<String> getPropositions() {
        return propositions;
    }

}
