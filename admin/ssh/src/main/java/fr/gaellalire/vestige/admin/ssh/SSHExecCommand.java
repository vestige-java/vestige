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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

import org.apache.sshd.server.Command;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.gaellalire.vestige.admin.command.CommandLineParser;
import fr.gaellalire.vestige.admin.command.DefaultCommandContext;
import fr.gaellalire.vestige.admin.command.VestigeCommandExecutor;
import fr.gaellalire.vestige.job.JobController;
import fr.gaellalire.vestige.job.JobListener;
import fr.gaellalire.vestige.job.TaskListener;

/**
 * @author Gael Lalire
 */
public class SSHExecCommand implements Command, Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(SSHExecCommand.class);

    private static final AtomicInteger THREAD_COUNT = new AtomicInteger(0);

    private InputStream in;

    private PrintWriter out;

    private PrintWriter err;

    private ExitCallback callback;

    private VestigeCommandExecutor vestigeCommand;

    private String lines;

    public SSHExecCommand(final VestigeCommandExecutor vestigeCommand, final String lines) {
        this.vestigeCommand = vestigeCommand;
        this.lines = lines;
    }

    public void start(final Environment env) throws IOException {
        new Thread(this, "vestige-exec-" + THREAD_COUNT.incrementAndGet()).start();
    }

    public void setOutputStream(final OutputStream out) {
        this.out = new PrintWriter(new LineFilterOutputStream(out), true);
    }

    public void setInputStream(final InputStream in) {
        this.in = in;
    }

    public void setExitCallback(final ExitCallback callback) {
        this.callback = callback;
    }

    public void setErrorStream(final OutputStream err) {
        this.err = new PrintWriter(new LineFilterOutputStream(err), true);
    }

    public void destroy() {

    }

    public void run() {
        DefaultCommandContext defaultCommandContext = new DefaultCommandContext();
        defaultCommandContext.setOut(out);
        final Thread currentThread = Thread.currentThread();
        defaultCommandContext.setJobListener(new JobListener() {

            @Override
            public TaskListener taskAdded(final String description) {
                return null;
            }

            @Override
            public void jobDone() {
                LockSupport.unpark(currentThread);
            }
        });
        CommandLineParser commandLineParser = new CommandLineParser();
        try {
            try {
                String[] lineArray = lines.split("\\r?\\n");
                for (String line : lineArray) {
                    commandLineParser.setCommandLine(line);
                    List<String> arguments = new ArrayList<String>();
                    if (commandLineParser.nextArgument()) {
                        String argument = commandLineParser.getUnescapedValue();
                        if ("exit".equals(argument)) {
                            out.println("logout");
                            break;
                        }
                        arguments.add(argument);
                        while (commandLineParser.nextArgument()) {
                            arguments.add(commandLineParser.getUnescapedValue());
                        }
                    } else {
                        continue;
                    }
                    JobController jobController = vestigeCommand.exec(defaultCommandContext, arguments);
                    if (jobController != null) {
                        while (!jobController.isDone()) {
                            LockSupport.park();
                        }
                    }
                }
                callback.onExit(0);
            } finally {
                out.close();
                err.close();
                in.close();
            }
        } catch (Exception e) {
            LOGGER.warn("Exception with ssh client", e);
        }
    }

}
