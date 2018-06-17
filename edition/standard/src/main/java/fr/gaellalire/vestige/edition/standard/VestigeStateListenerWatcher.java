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

package fr.gaellalire.vestige.edition.standard;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Gael Lalire
 */
public class VestigeStateListenerWatcher extends Thread {

    private static final Logger LOGGER = LoggerFactory.getLogger(VestigeStateListenerWatcher.class);

    private SocketChannel socketChannel;

    public VestigeStateListenerWatcher(final SocketChannel socketChannel) {
        super("vestige-stateListener-watcher");
        setDaemon(true);
        this.socketChannel = socketChannel;
    }

    @Override
    public void run() {
        try {
            Selector selector = Selector.open();
            socketChannel.register(selector, SelectionKey.OP_READ);
            int r = 0;
            ByteBuffer allocate = ByteBuffer.allocate(1);
            while (r != -1) {
                selector.select();
                r = socketChannel.read(allocate);
                allocate.clear();
            }
        } catch (Exception e) {
            LOGGER.info("Listener error", e);
        } finally {
            LOGGER.info("Shutdown JVM because the listener closes its connection");
            System.exit(0);
        }
    }

}
