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

package com.googlecode.vestige.admin.telnet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.googlecode.vestige.admin.command.VestigeCommandExecutor;

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
        try {
            String readLine = bufferedReader.readLine();
            while (readLine != null) {
                String[] args = readLine.split("\\s+");
                if (args.length == 0) {
                    readLine = bufferedReader.readLine();
                    continue;
                }
                if (args[0].equals("exit")) {
                    break;
                }
                commandExecutor.exec(printWriter, args);
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
