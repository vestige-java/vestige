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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.concurrent.atomic.AtomicInteger;

import jline.console.ConsoleReader;

import org.apache.sshd.server.Command;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Gael Lalire
 */
public class SSHShellCommand implements Command, Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(SSHShellCommand.class);

    private static final AtomicInteger THREAD_COUNT = new AtomicInteger(0);

    private InputStream in;

    private OutputStream outs;

    private PrintWriter out;

    private PrintWriter err;

    private ConsoleReader consoleReader;

    private ExitCallback callback;

    private SSHShellCommandFactory commandFactory;

    public SSHShellCommand(final SSHShellCommandFactory commandFactory) {
        this.commandFactory = commandFactory;
    }

    public void start(final Environment env) throws IOException {
        consoleReader = new ConsoleReader(in, outs, new SSHTerminal());
        consoleReader.addCompleter(commandFactory.getCompleter());
        new Thread(this, "vestige-shell-" + THREAD_COUNT.incrementAndGet()).start();
    }

    public void setOutputStream(final OutputStream out) {
        this.outs = new LineFilterOutputStream(out);
        this.out = new PrintWriter(outs, true);
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
        try {
            try {
                String readLine = null;
                do {
                    readLine = consoleReader.readLine("vestige:~ admin$ ");
                    if (readLine == null) {
                        out.println("logout");
                        break;
                    }
                    if (readLine.length() == 0) {
                        continue;
                    }
                    String[] split = readLine.split("\\s+");
                    if (split.length == 0) {
                        continue;
                    }
                    if (split[0].equals("exit")) {
                        out.println("logout");
                        break;
                    }
                    commandFactory.getVestigeCommandExecutor().exec(out, split);
                } while (true);
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
