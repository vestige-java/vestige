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

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Gael Lalire
 */
public class NIOWriterVestigeStateListener implements VestigeStateListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(NIOWriterVestigeStateListener.class);

    private static final Charset UTF8_CHARSET = Charset.forName("UTF-8");

    private ByteBuffer headerByteBuffer = ByteBuffer.allocate(4);

    private SocketChannel socketChannel;

    private Selector selector;

    public NIOWriterVestigeStateListener(final SocketChannel socketChannel) throws IOException {
        this.socketChannel = socketChannel;
        selector = Selector.open();
        socketChannel.register(selector, SelectionKey.OP_WRITE);
    }

    public void write(final String s) {
        write(s, UTF8_CHARSET);
    }

    public void write(final String s, final Charset charset) {
        try {
            ByteBuffer wrap = ByteBuffer.wrap(s.getBytes(charset));
            headerByteBuffer.clear();
            int limit = wrap.limit();
            headerByteBuffer.putInt(limit);
            headerByteBuffer.flip();
            socketChannel.write(headerByteBuffer);
            while (headerByteBuffer.hasRemaining()) {
                selector.select();
                socketChannel.write(headerByteBuffer);
            }
            socketChannel.write(wrap);
            while (wrap.hasRemaining()) {
                selector.select();
                socketChannel.write(wrap);
            }
        } catch (IOException e) {
            LOGGER.trace("IOException", e);
            LOGGER.error("Unable to send {}", s);
        }
    }

    @Override
    public void starting() {
        write("Starting");
    }

    @Override
    public void started() {
        write("Started");
    }

    @Override
    public void failed() {
        write("Failed");
    }

    @Override
    public void stopping() {
        write("Stopping");
    }

    @Override
    public void stopped() {
        write("Stopped");
    }

    @Override
    public void webAdminAvailable(final String url) {
        write("Web " + url);
    }

    @Override
    public void config(final File file) {
        write("Config " + file.getAbsolutePath());
    }

    @Override
    public void certificateAuthorityGenerated(final File file) {
        write("CA " + file.getAbsolutePath());
    }

    @Override
    public void clientP12Generated(final File file) {
        write("ClientP12 " + file.getAbsolutePath());
    }

}
