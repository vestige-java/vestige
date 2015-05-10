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
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.googlecode.vestige.admin.command.argument.Argument;
import com.googlecode.vestige.admin.command.argument.ParseException;
import com.googlecode.vestige.application.ApplicationManager;
import com.googlecode.vestige.platform.VestigePlatform;

/**
 * @author Gael Lalire
 */
public class VestigeCommandExecutor {

    private Map<String, Command> commandByNames;

    public VestigeCommandExecutor(final ApplicationManager applicationManager, final VestigePlatform vestigePlatform) {
        List<Command> commands = Arrays.asList(new Memory(), new GC(), new ForceGC(), new Install(applicationManager), new MakeRepo(applicationManager),
                new RemoveRepo(applicationManager), new Start(applicationManager), new Stop(applicationManager), new Uninstall(
                        applicationManager), new ListCommand(applicationManager), new AutoMigrate(applicationManager),
                new AutoMigrateLevel(applicationManager), new Migrate(applicationManager), new ClassLoaders(applicationManager), new Platform(vestigePlatform));
        commandByNames = new TreeMap<String, Command>();
        for (Command command : commands) {
            commandByNames.put(command.getName(), command);
        }
    }

    public Map<String, Command> getCommandByNames() {
        return commandByNames;
    }

    public void exec(final PrintWriter out, final String... args) {
        if (args.length == 0) {
            return;
        }
        if (args[0].equals("help")) {
            if (args.length != 1) {
                Command command = commandByNames.get(args[1]);
                if (command == null) {
                    out.println("no help found for command: " + args[1]);
                    return;
                }
                out.print("SYNOPSIS: ");
                out.print(command.getName());
                for (Argument argument : command.getArguments()) {
                    out.print(' ');
                    out.print(argument.getName());
                }
                out.println();
                out.print("DESCRIPTION: ");
                out.println(command.getDesc());
            } else {
                Collection<Command> commands = commandByNames.values();
                out.println("Command list:");
                for (Command command : commands) {
                    out.print(" - ");
                    out.print(command.getName());
                    for (Argument argument : command.getArguments()) {
                        out.print(' ');
                        out.print(argument.getName());
                    }
                    out.println();
                }
                out.println("Type `help <command>' for more information");
            }
            return;
        }
        Command command = commandByNames.get(args[0]);
        if (command == null) {
            out.println("command not found: " + args[0]);
            out.println("Type `help' for command listing");
            return;
        }
        int i = 1;
        try {
            for (Argument argument : command.getArguments()) {
                if (i == args.length) {
                    out.println("Missing arg : " + argument.getName());
                    out.println("`help " + command.getName() + "' for more information");
                    return;
                }
                try {
                    argument.parse(args[i]);
                } catch (ParseException e) {
                    e.printStackTrace(out);
                    Collection<String> propose;
                    try {
                        propose = argument.propose();
                        if (propose != null) {
                            if (propose.size() == 0) {
                                out.println("No valid value");
                            } else {
                                out.println("Valid values are " + propose);
                            }
                        }
                    } catch (ParseException pe) {
                        out.println("Unable to get valid value");
                        pe.printStackTrace(out);
                    }
                    out.println("`help " + command.getName() + "' for more information");
                    return;
                }
                i++;
            }
            command.execute(out);
        } finally {
            for (Argument argument : command.getArguments()) {
                argument.reset();
            }
        }
    }

}
