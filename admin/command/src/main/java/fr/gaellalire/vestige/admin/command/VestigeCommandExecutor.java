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

package fr.gaellalire.vestige.admin.command;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import fr.gaellalire.vestige.admin.command.argument.Argument;
import fr.gaellalire.vestige.admin.command.argument.ParseException;
import fr.gaellalire.vestige.admin.command.argument.SimpleProposeContext;
import fr.gaellalire.vestige.application.manager.ApplicationManager;
import fr.gaellalire.vestige.job.JobController;
import fr.gaellalire.vestige.job.JobManager;

/**
 * @author Gael Lalire
 */
public class VestigeCommandExecutor {

    private Map<String, Command> commandByNames;

    public VestigeCommandExecutor(final JobManager jobManager, final ApplicationManager applicationManager) {
        List<Command> commands = Arrays.asList(new Memory(), new GC(), new ForceGC(), new Install(applicationManager), new TrustedInstall(applicationManager),
                new MakeRepo(applicationManager), new RemoveRepo(applicationManager), new Start(applicationManager), new Status(applicationManager), new Stop(applicationManager),
                new Discard(applicationManager), new GenerateApplicationMetadata(applicationManager), new PGPSign(applicationManager), new PGPDefaultSign(applicationManager),
                new Uninstall(applicationManager), new ListCommand(applicationManager), new AutoMigrate(applicationManager), new AutoMigrateLevel(applicationManager),
                new Migrate(applicationManager), new ClassLoaders(applicationManager), new AutoStart(applicationManager), new DescriptorReload(applicationManager),
                new PSCommand(jobManager), new Kill(jobManager), new TasksCommand(jobManager));
        commandByNames = new TreeMap<String, Command>();
        for (Command command : commands) {
            commandByNames.put(command.getName(), command);
        }
    }

    public void addCommand(final Command command) {
        commandByNames.put(command.getName(), command);
    }

    public Map<String, Command> getCommandByNames() {
        return commandByNames;
    }

    public JobController exec(final CommandContext commandContext, final List<String> args) {
        if (args.size() == 0) {
            return null;
        }
        PrintWriter out = commandContext.getOut();
        if (args.get(0).equals("help")) {
            if (args.size() != 1) {
                Command command = commandByNames.get(args.get(1));
                if (command == null) {
                    out.println("no help found for command: " + args.get(1));
                    return null;
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
            return null;
        }
        Command command = commandByNames.get(args.get(0));
        if (command == null) {
            out.println("command not found: " + args.get(0));
            out.println("Type `help' for command listing");
            return null;
        }
        synchronized (command) {
            int i = 1;
            try {
                for (Argument argument : command.getArguments()) {
                    if (i == args.size()) {
                        out.println("Missing arg : " + argument.getName());
                        out.println("`help " + command.getName() + "' for more information");
                        return null;
                    }
                    try {
                        argument.parse(args.get(i));
                    } catch (ParseException e) {
                        e.printStackTrace(out);
                        try {
                            SimpleProposeContext simpleProposeContext = commandContext.getSimpleProposeContext();
                            simpleProposeContext.reset();
                            argument.propose(simpleProposeContext);
                            Set<String> propositions = simpleProposeContext.getPropositions();
                            if (propositions.size() == 0) {
                                out.println("No valid value");
                            } else {
                                out.println("Valid values are " + propositions);
                            }
                        } catch (ParseException pe) {
                            out.println("Unable to get valid value");
                            pe.printStackTrace(out);
                        }
                        out.println("`help " + command.getName() + "' for more information");
                        return null;
                    }
                    i++;
                }
                return command.execute(commandContext);
            } finally {
                for (Argument argument : command.getArguments()) {
                    argument.reset();
                }
            }
        }
    }

}
