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

package com.googlecode.vestige.jvm_enhancer;

import java.text.MessageFormat;
import java.util.WeakHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.btr.proxy.util.Logger.LogBackEnd;
import com.btr.proxy.util.Logger.LogLevel;

/**
 * @author Gael Lalire
 */
public class SLF4JBackend implements LogBackEnd {

    private WeakHashMap<Class<?>, Logger> map = new WeakHashMap<Class<?>, Logger>();

    public void log(final Class<?> clazz, final LogLevel loglevel, final String msg, final Object... params) {
        Logger logger;
        synchronized (map) {
            logger = map.get(clazz);
            if (logger == null) {
                logger = LoggerFactory.getLogger(clazz);
                map.put(clazz, logger);
            }
        }
        switch (loglevel) {
        case TRACE:
            if (!logger.isTraceEnabled()) {
                return;
            }
            break;
        case DEBUG:
            if (!logger.isDebugEnabled()) {
                return;
            }
            break;
        case INFO:
            if (!logger.isInfoEnabled()) {
                return;
            }
            break;
        case WARNING:
            if (!logger.isWarnEnabled()) {
                return;
            }
            break;
        case ERROR:
            if (!logger.isErrorEnabled()) {
                return;
            }
            break;
        default:
            throw new Error("Unknown level " + loglevel);
        }
        String formattedMessage = MessageFormat.format(msg, params);
        switch (loglevel) {
        case TRACE:
            logger.trace(formattedMessage);
            break;
        case DEBUG:
            logger.debug(formattedMessage);
            break;
        case INFO:
            logger.info(formattedMessage);
            break;
        case WARNING:
            logger.warn(formattedMessage);
            break;
        case ERROR:
            logger.error(formattedMessage);
            break;
        default:
            throw new Error("Unknown level " + loglevel);
        }
    }

    public boolean isLogginEnabled(final LogLevel logLevel) {
        return true;
    }

}
