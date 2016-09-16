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

package fr.gaellalire.vestige.admin.ssh;

import java.io.File;
import java.util.Map;

import jline.console.completer.Completer;

import org.apache.sshd.common.Factory;
import org.apache.sshd.server.Command;

import fr.gaellalire.vestige.admin.command.VestigeCommandExecutor;

/**
 * @author Gael Lalire
 */
public class SSHShellCommandFactory implements Factory<Command> {

    private VestigeCommandExecutor vestigeCommandExecutor;

    private Completer completer;

    private File historyFile;

    public SSHShellCommandFactory(final VestigeCommandExecutor vestigeCommandExecutor, final File historyFile) {
        this.vestigeCommandExecutor = vestigeCommandExecutor;
        this.historyFile = historyFile;
        Map<String, fr.gaellalire.vestige.admin.command.Command> commandByNames = vestigeCommandExecutor.getCommandByNames();
        completer = new SSHCommandCompleter(commandByNames);
    }

    public Command create() {
        return new SSHShellCommand(this, historyFile);
    }

    public Completer getCompleter() {
        return completer;
    }

    public VestigeCommandExecutor getVestigeCommandExecutor() {
        return vestigeCommandExecutor;
    }

}
