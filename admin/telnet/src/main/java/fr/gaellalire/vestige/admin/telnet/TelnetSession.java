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

package fr.gaellalire.vestige.admin.telnet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.gaellalire.vestige.admin.command.CommandLineParser;
import fr.gaellalire.vestige.admin.command.DefaultCommandContext;
import fr.gaellalire.vestige.admin.command.VestigeCommandExecutor;

/**
 * @author Gael Lalire
 */
public class TelnetSession implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(TelnetSession.class);

    private BufferedReader bufferedReader;

    private PrintWriter printWriter;

    private VestigeCommandExecutor commandExecutor;

    private Thread thread;

    public TelnetSession(final VestigeCommandExecutor commandExecutor, final BufferedReader bufferedReader,
            final PrintWriter printWriter) {
        this.bufferedReader = bufferedReader;
        this.printWriter = printWriter;
        this.commandExecutor = commandExecutor;
    }

    public void start() {
        thread = new Thread(this);
        thread.start();
    }

    public void stop() throws InterruptedException, IOException {
        bufferedReader.close();
        printWriter.close();
        thread.interrupt();
        thread.join();
    }

    public void run() {
        DefaultCommandContext defaultCommandContext = new DefaultCommandContext();
        CommandLineParser commandLineParser = new CommandLineParser();
        defaultCommandContext.setOut(printWriter);
        try {
            String readLine = bufferedReader.readLine();
            while (readLine != null) {
                commandLineParser.setCommandLine(readLine);
                List<String> arguments = new ArrayList<String>();
                if (commandLineParser.nextArgument()) {
                    String argument = commandLineParser.getUnescapedValue();
                    if ("exit".equals(argument)) {
                        printWriter.println("logout");
                        break;
                    }
                    arguments.add(argument);
                    while (commandLineParser.nextArgument()) {
                        arguments.add(commandLineParser.getUnescapedValue());
                    }
                } else {
                    continue;
                }
                commandExecutor.exec(defaultCommandContext, arguments);
                readLine = bufferedReader.readLine();
            }
        } catch (IOException e) {
            LOGGER.error("IOException", e);
        } finally {
            try {
                bufferedReader.close();
            } catch (IOException e) {
                LOGGER.warn("IOException", e);
            }
            printWriter.close();
        }

    }

}
