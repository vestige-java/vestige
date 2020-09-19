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

package fr.gaellalire.vestige.system.logger;

import java.lang.Thread.UncaughtExceptionHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.gaellalire.vestige.core.StackedHandler;

/**
 * @author Gael Lalire
 */
public class SLF4JUncaughtExceptionHandler implements UncaughtExceptionHandler, StackedHandler<UncaughtExceptionHandler> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SLF4JUncaughtExceptionHandler.class);

    private UncaughtExceptionHandler nextHandler;

    public SLF4JUncaughtExceptionHandler(final UncaughtExceptionHandler nextHandler) {
        this.nextHandler = nextHandler;
    }

    @Override
    public void uncaughtException(final Thread t, final Throwable e) {
        if (LOGGER.isErrorEnabled()) {
            LOGGER.error("Uncaught exception", e);
        }
    }

    @Override
    public UncaughtExceptionHandler getNextHandler() {
        return nextHandler;
    }

    @Override
    public void setNextHandler(final UncaughtExceptionHandler nextHandler) {
        this.nextHandler = nextHandler;
    }

}
