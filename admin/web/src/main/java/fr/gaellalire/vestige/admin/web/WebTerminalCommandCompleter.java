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

package fr.gaellalire.vestige.admin.web;

import java.util.List;
import java.util.Map;

import net.minidev.json.JSONArray;
import fr.gaellalire.vestige.admin.command.Command;
import fr.gaellalire.vestige.admin.command.CommandLineParser;
import fr.gaellalire.vestige.admin.command.argument.Argument;
import fr.gaellalire.vestige.admin.command.argument.CommandLineProposeContext;
import fr.gaellalire.vestige.admin.command.argument.ParseException;

/**
 * @author Gael Lalire
 */
public class WebTerminalCommandCompleter {

    private Map<String, Command> commandByName;

    private CommandLineParser commandLineParser;

    private CommandLineProposeContext defaultProposeContext;

    public WebTerminalCommandCompleter(final Map<String, Command> commandByName) {
        this.commandByName = commandByName;
        commandLineParser = new CommandLineParser();
        defaultProposeContext = new CommandLineProposeContext(commandLineParser);
    }

    public int complete(final String totalBuffer, final int cursor, final JSONArray candidates) {
        commandLineParser.setCommandLine(totalBuffer.substring(0, cursor));

        if (!commandLineParser.nextArgument(true)) {
            defaultProposeContext.reset("");
            for (String match : commandByName.keySet()) {
                defaultProposeContext.addProposition(match);
            }
            defaultProposeContext.addProposition("help");
            candidates.addAll(defaultProposeContext.getPropositions());
            return 0;
        }
        String commandString = commandLineParser.getUnescapedValue();
        String lastString = "";
        boolean hasArgument = commandLineParser.nextArgument(true);
        if (!hasArgument) {
            defaultProposeContext.reset(commandString);
            // complete the command
            for (String match : commandByName.keySet()) {
                defaultProposeContext.addProposition(match);
            }
            defaultProposeContext.addProposition("help");
            candidates.addAll(defaultProposeContext.getPropositions());
            if (candidates.isEmpty()) {
                return -1;
            }
            return commandLineParser.getStart();
        } else {
            lastString = commandLineParser.getUnescapedValue();
        }
        if (commandString.equals("help")) {
            defaultProposeContext.reset(lastString);

            for (String match : commandByName.keySet()) {
                defaultProposeContext.addProposition(match);
            }
            candidates.addAll(defaultProposeContext.getPropositions());
            if (candidates.isEmpty()) {
                return -1;
            }
            if (!hasArgument) {
                return cursor;
            }
            return commandLineParser.getStart();
        }

        Command command = commandByName.get(commandString);
        if (command == null) {
            return -1;
        }

        List<? extends Argument> arguments = command.getArguments();
        int size = arguments.size();
        if (size == 0) {
            return -1;
        }
        try {
            Argument lastArgument = arguments.get(0);
            int argumentPos = 1;
            if (commandLineParser.nextArgument(true)) {
                // there is another argument or spaces after
                // => lastString argument is complete => we can call parse method
                if (argumentPos == size) {
                    return -1;
                }
                try {
                    lastArgument.parse(lastString);
                } catch (ParseException e) {
                    return -1;
                }
                lastArgument = arguments.get(argumentPos);
                lastString = commandLineParser.getUnescapedValue();
                while (commandLineParser.nextArgument(true)) {
                    argumentPos++;
                    if (argumentPos == size) {
                        return -1;
                    }
                    try {
                        lastArgument.parse(lastString);
                    } catch (ParseException e) {
                        return -1;
                    }
                    lastArgument = arguments.get(argumentPos);
                    lastString = commandLineParser.getUnescapedValue();
                }
            }

            defaultProposeContext.reset(lastString);
            try {
                lastArgument.propose(defaultProposeContext);
            } catch (ParseException e) {
                return -1;
            }
            int hiddenContext = defaultProposeContext.getEscapePrefixHiddenLength();
            candidates.addAll(defaultProposeContext.getPropositions());

            if (candidates.isEmpty()) {
                return -1;
            }
            return commandLineParser.getStart() + hiddenContext;
        } finally {
            for (Argument argument : arguments) {
                argument.reset();
            }
        }
    }

}
