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

package com.googlecode.vestige.platform.system.interceptor;

import java.io.IOException;
import java.io.InputStream;

import com.googlecode.vestige.core.StackedHandler;

/**
 * @author Gael Lalire
 */
public abstract class VestigeInputStream extends InputStream implements StackedHandler<InputStream> {

    private InputStream nextHandler;

    public VestigeInputStream(final InputStream nextHandler) {
        this.nextHandler = nextHandler;
    }

    public abstract InputStream getInputStream();

    @Override
    public int read() throws IOException {
        return getInputStream().read();
    }

    @Override
    public int available() throws IOException {
        return getInputStream().available();
    }

    @Override
    public void close() throws IOException {
        getInputStream().close();
    }

    @Override
    public boolean equals(final Object obj) {
        return getInputStream().equals(obj);
    }

    @Override
    public int hashCode() {
        return getInputStream().hashCode();
    }

    @Override
    public void mark(final int readlimit) {
        getInputStream().mark(readlimit);
    }

    @Override
    public boolean markSupported() {
        return getInputStream().markSupported();
    }

    @Override
    public int read(final byte[] b) throws IOException {
        return getInputStream().read(b);
    }

    @Override
    public int read(final byte[] b, final int off, final int len) throws IOException {
        return getInputStream().read(b, off, len);
    }

    @Override
    public void reset() throws IOException {
        getInputStream().reset();
    }

    @Override
    public long skip(final long n) throws IOException {
        return getInputStream().skip(n);
    }

    @Override
    public String toString() {
        return getInputStream().toString();
    }

    public InputStream getNextHandler() {
        return nextHandler;
    }

    public void setNextHandler(final InputStream nextHandler) {
        this.nextHandler = nextHandler;
    }

}
