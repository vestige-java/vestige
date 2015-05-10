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
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Locale;

import com.googlecode.vestige.core.StackedHandler;

/**
 * @author Gael Lalire
 */
public abstract class VestigePrintStream extends PrintStream implements StackedHandler<PrintStream> {

    public static final OutputStream NULL_OUTPUT_STREAM = new OutputStream() {

        @Override
        public void write(final int b) throws IOException {
        }
    };

    private PrintStream nextHandler;

    public abstract PrintStream getPrintStream();

    public VestigePrintStream(final PrintStream nextHandler) {
        super(NULL_OUTPUT_STREAM);
        this.nextHandler = nextHandler;
    }

    @Override
    public PrintStream append(final char c) {
        return getPrintStream().append(c);
    }

    @Override
    public PrintStream append(final CharSequence csq) {
        return getPrintStream().append(csq);
    }

    @Override
    public PrintStream append(final CharSequence csq, final int start, final int end) {
        return getPrintStream().append(csq, start, end);
    }

    @Override
    public boolean checkError() {
        return getPrintStream().checkError();
    }

    @Override
    public void close() {
        getPrintStream().close();
    }

    @Override
    public boolean equals(final Object obj) {
        return getPrintStream().equals(obj);
    }

    @Override
    public void flush() {
        getPrintStream().flush();
    }

    @Override
    public PrintStream format(final Locale l, final String format, final Object... args) {
        return getPrintStream().format(l, format, args);
    }

    @Override
    public PrintStream format(final String format, final Object... args) {
        return getPrintStream().format(format, args);
    }

    @Override
    public int hashCode() {
        return getPrintStream().hashCode();
    }

    @Override
    public void print(final boolean b) {
        getPrintStream().print(b);
    }

    @Override
    public void print(final char c) {
        getPrintStream().print(c);
    }

    @Override
    public void print(final char[] s) {
        getPrintStream().print(s);
    }

    @Override
    public void print(final double d) {
        getPrintStream().print(d);
    }

    @Override
    public void print(final float f) {
        getPrintStream().print(f);
    }

    @Override
    public void print(final int i) {
        getPrintStream().print(i);
    }

    @Override
    public void print(final long l) {
        getPrintStream().print(l);
    }

    @Override
    public void print(final Object obj) {
        getPrintStream().print(obj);
    }

    @Override
    public void print(final String s) {
        getPrintStream().print(s);
    }

    @Override
    public PrintStream printf(final Locale l, final String format, final Object... args) {
        return getPrintStream().printf(l, format, args);
    }

    @Override
    public PrintStream printf(final String format, final Object... args) {
        return getPrintStream().printf(format, args);
    }

    @Override
    public void println() {
        getPrintStream().println();
    }

    @Override
    public void println(final boolean x) {
        getPrintStream().println(x);
    }

    @Override
    public void println(final char x) {
        getPrintStream().println(x);
    }

    @Override
    public void println(final char[] x) {
        getPrintStream().println(x);
    }

    @Override
    public void println(final double x) {
        getPrintStream().println(x);
    }

    @Override
    public void println(final float x) {
        getPrintStream().println(x);
    }

    @Override
    public void println(final int x) {
        getPrintStream().println(x);
    }

    @Override
    public void println(final long x) {
        getPrintStream().println(x);
    }

    @Override
    public void println(final Object x) {
        getPrintStream().println(x);
    }

    @Override
    public void println(final String x) {
        getPrintStream().println(x);
    }

    @Override
    public String toString() {
        return getPrintStream().toString();
    }

    @Override
    public void write(final byte[] b) throws IOException {
        getPrintStream().write(b);
    }

    @Override
    public void write(final byte[] buf, final int off, final int len) {
        getPrintStream().write(buf, off, len);
    }

    @Override
    public void write(final int b) {
        getPrintStream().write(b);
    }

    public PrintStream getNextHandler() {
        return nextHandler;
    }

    public void setNextHandler(final PrintStream nextHandler) {
        this.nextHandler = nextHandler;
    }

}
