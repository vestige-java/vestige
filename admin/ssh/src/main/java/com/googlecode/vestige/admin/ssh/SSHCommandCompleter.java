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

package com.googlecode.vestige.admin.ssh;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import jline.console.completer.Completer;

import com.googlecode.vestige.admin.command.Command;
import com.googlecode.vestige.admin.command.argument.Argument;
import com.googlecode.vestige.admin.command.argument.ParseException;

/**
 * @author Gael Lalire
 */
public class SSHCommandCompleter implements Completer {

    private Map<String, Command> commandByName;

    public SSHCommandCompleter(final Map<String, Command> commandByName) {
        this.commandByName = commandByName;
    }

    public int complete(final String totalBuffer, final int cursor, final List<CharSequence> candidates) {
        String buffer = totalBuffer.substring(0, cursor);
        if (buffer.length() == 0) {
            candidates.addAll(commandByName.keySet());
            candidates.add("help");
            candidates.add("exit");
            return 0;
        }
        String[] split = buffer.split("\\s+");
        if (split.length == 0) {
            return -1;
        }
        if (split.length == 1 && buffer.length() == split[0].length()) {
            for (String match : commandByName.keySet()) {
                if (!match.startsWith(buffer)) {
                    continue;
                }
                candidates.add(match);
            }
            if ("help".startsWith(buffer)) {
                candidates.add("help");
            }
            if ("exit".startsWith(buffer)) {
                candidates.add("exit");
            }
            if (candidates.size() == 1) {
                candidates.set(0, candidates.get(0) + " ");
            }
            if (candidates.isEmpty()) {
                return -1;
            }
            return 0;
        }
        if (split[0].equals("help")) {
            String lastString;
            if (split.length == 1) {
                lastString = "";
            } else if (split.length == 2) {
                lastString = split[1];
            } else {
                return -1;
            }
            for (String match : commandByName.keySet()) {
                if (!match.startsWith(lastString)) {
                    continue;
                }
                candidates.add(match);
            }
            if (candidates.size() == 1) {
                candidates.set(0, candidates.get(0) + " ");
            }
            if (candidates.isEmpty()) {
                return -1;
            }
            return buffer.lastIndexOf(lastString);
        }
        Command command = commandByName.get(split[0]);
        if (command == null) {
            return -1;
        }

        List<? extends Argument> arguments = command.getArguments();
        int size = arguments.size();
        if (size == 0) {
            return -1;
        }
        int argumentsLength = split.length - 1;
        try {
            String lastString;
            Argument lastArgument;

            if (argumentsLength == 0) {
                lastString = "";
                lastArgument = arguments.get(0);
            } else {
                // each args expect the last
                for (int i = 0; i < argumentsLength - 1; i++) {
                    if (i == size) {
                        return -1;
                    }
                    Argument argument = arguments.get(i);
                    try {
                        argument.parse(split[i + 1]);
                    } catch (ParseException e) {
                        return -1;
                    }
                }
                if (argumentsLength - 1 == size) {
                    return -1;
                }
                // last arg
                lastString = split[argumentsLength];
                lastArgument = arguments.get(argumentsLength - 1);

                if (buffer.endsWith(" ")) {
                    // last arg is valid
                    try {
                        lastArgument.parse(lastString);
                    } catch (ParseException e) {
                        return -1;
                    }
                    if (argumentsLength == size) {
                        return -1;
                    }
                    lastString = "";
                    lastArgument = arguments.get(argumentsLength);
                }
            }
            Collection<String> propose;
            try {
                propose = lastArgument.propose();
            } catch (ParseException e) {
                return -1;
            }
            if (propose == null) {
                return -1;
            }
            for (String match : propose) {
                if (!match.startsWith(lastString)) {
                    continue;
                }
                candidates.add(match);
            }
            if (candidates.size() == 1) {
                candidates.set(0, candidates.get(0) + " ");
            }

            if (candidates.isEmpty()) {
                return -1;
            }
            return buffer.lastIndexOf(lastString);
        } finally {
            for (Argument argument : arguments) {
                argument.reset();
            }
        }

    }

}
