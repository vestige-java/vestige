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

package com.googlecode.vestige.platform.logger;


import org.slf4j.LoggerFactory;

import com.googlecode.vestige.core.StackedHandler;
import com.googlecode.vestige.core.logger.VestigeLogger;
import com.googlecode.vestige.core.logger.VestigeLoggerFactory;

/**
 * @author Gael Lalire
 */
public class SLF4JLoggerFactoryAdapter extends VestigeLoggerFactory implements StackedHandler<VestigeLoggerFactory> {

    private VestigeLoggerFactory nextHandler;

    @Override
    public VestigeLogger createLogger(final String name) {
        return new SLF4JLoggerAdapter(LoggerFactory.getLogger(name));
    }

    public VestigeLoggerFactory getNextHandler() {
        return nextHandler;
    }

    public void setNextHandler(final VestigeLoggerFactory nextHandler) {
        this.nextHandler = nextHandler;
    }

}
