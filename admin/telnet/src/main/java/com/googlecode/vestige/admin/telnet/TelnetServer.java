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
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.googlecode.vestige.admin.command.VestigeCommandExecutor;

/**
 * @author Gael Lalire
 */
public class TelnetServer implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(TelnetServer.class);

    private int port;

    private ServerSocket serverSocket;

    private Thread thread;

    private VestigeCommandExecutor commandExecutor;

    public TelnetServer(final VestigeCommandExecutor commandExecutor, final int port) throws IOException {
        this.commandExecutor = commandExecutor;
        this.port = port;
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        thread = new Thread(this);
        thread.start();
    }

    public void stop() throws IOException {
        serverSocket.close();
        thread.interrupt();
    }

    public void run() {
        while (true) {
            try {
                Socket accept = serverSocket.accept();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(accept.getInputStream(), "US-ASCII"));
                new TelnetSession(commandExecutor, bufferedReader, new PrintWriter(accept.getOutputStream(), true)).start();
            } catch (IOException e) {
                LOGGER.error("IOException", e);
            }
        }
    }

}
