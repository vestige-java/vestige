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

package com.googlecode.vestige.application;

/**
 * @author Gael Lalire
 */
public class ThreadStackTraceException extends Exception {

    private static final long serialVersionUID = 2594835488228389486L;

    private StackTraceElement[] elements;

    public ThreadStackTraceException(final Thread t) {
        super("StackTrace of " + t.getName());
        elements = t.getStackTrace();
    }

    @Override
    public StackTraceElement[] getStackTrace() {
        return elements;
    }

}
