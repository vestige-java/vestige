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

package fr.gaellalire.vestige.jvm_enhancer.runtime;

import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import com.btr.proxy.util.Logger.LogBackEnd;
import com.btr.proxy.util.Logger.LogLevel;

/**
 * @author Gael Lalire
 */
public class JULBackend implements LogBackEnd {

    private static final Logger LOGGER = Logger.getAnonymousLogger();

    public void log(final Class<?> clazz, final LogLevel loglevel, final String msg, final Object... params) {
        Level level;
        switch (loglevel) {
        case TRACE:
            level = Level.FINEST;
            break;
        case DEBUG:
            level = Level.FINE;
            break;
        case INFO:
            level = Level.INFO;
            break;
        case WARNING:
            level = Level.WARNING;
            break;
        case ERROR:
            level = Level.SEVERE;
            break;
        default:
            throw new Error("Unknown level " + loglevel);
        }
        LogRecord logRecord = new LogRecord(level, msg);
        logRecord.setLoggerName(clazz.getName());
        logRecord.setParameters(params);
        LOGGER.log(logRecord);
    }

    public boolean isLogginEnabled(final LogLevel logLevel) {
        return true;
    }

}
