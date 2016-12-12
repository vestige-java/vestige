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

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import fr.gaellalire.vestige.admin.command.CommandLineParser;

/**
 * @author Gael Lalire
 */
public class CommandLineProposeContext implements ProposeContext {

    private String prefix;

    private int unescapePrefixHiddenLength;

    private String escapeSuffixValue = "";

    private Set<String> propositions = new TreeSet<String>();

    private CommandLineParser commandLineParser;

    private boolean allUnterminated = true;

    public CommandLineProposeContext(final CommandLineParser commandLineParser) {
        this.commandLineParser = commandLineParser;
    }

    @Override
    public String getPrefix() {
        return prefix;
    }

    @Override
    public void setUnescapePrefixHiddenLength(final int unescapePrefixHiddenLength) {
        this.unescapePrefixHiddenLength = unescapePrefixHiddenLength;
        escapeSuffixValue = commandLineParser.getEscapeSuffixValue(unescapePrefixHiddenLength);
    }

    public int getEscapePrefixHiddenLength() {
        return commandLineParser.getEnd() - commandLineParser.getStart() - escapeSuffixValue.length();
    }

    public boolean canBeAdded(final String proposition) {
        if (!proposition.startsWith(prefix)) {
            return false;
        }
        if (commandLineParser.getMode() == 4) {
            int prefixLength = prefix.length();
            if (proposition.length() > prefixLength) {
                switch (proposition.charAt(prefixLength)) {
                case '\\':
                    break;
                case '\"':
                    break;
                default:
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void addProposition(final String proposition) {
        if (canBeAdded(proposition)) {
            propositions.add(escapeSuffixValue + CommandLineParser.escape(commandLineParser.getMode(), proposition.substring(prefix.length())));
            allUnterminated = false;
        }
    }

    @Override
    public void addUnterminatedProposition(final String proposition) {
        if (canBeAdded(proposition)) {
            propositions.add(escapeSuffixValue + CommandLineParser.escape(commandLineParser.getMode(), proposition.substring(prefix.length())));
        }
    }

    public void addPropositions(final List<CharSequence> candidates) {
        if (propositions.size() == 1 && !allUnterminated) {
            candidates.add(propositions.iterator().next() + " ");
        } else {
            candidates.addAll(propositions);
        }
    }

    public Set<String> getPropositions() {
        if (propositions.size() == 1 && !allUnterminated) {
            return Collections.singleton(propositions.iterator().next() + " ");
        } else {
            return propositions;
        }
    }

    public void reset(final String prefix) {
        this.prefix = prefix;
        unescapePrefixHiddenLength = 0;
        escapeSuffixValue = commandLineParser.getEscapeSuffixValue(unescapePrefixHiddenLength);
        allUnterminated = true;
        propositions.clear();
    }

}
