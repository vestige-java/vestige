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

import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.slf4j.Logger;

import com.googlecode.vestige.core.logger.VestigeLogger;

/**
 * @author Gael Lalire
 */
public class SLF4JLoggerAdapter implements VestigeLogger {

    private Logger logger;

    public SLF4JLoggerAdapter(final Logger logger) {
        this.logger = logger;
    }

    private void doLog(final Level level, final String msg) {
        int levelValue = level.intValue();
        if (levelValue >= Level.INFO.intValue()) {
            if (levelValue >= Level.SEVERE.intValue()) {
                if (levelValue != Level.OFF.intValue()) {
                    logger.error(msg);
                }
            } else {
                if (levelValue >= Level.WARNING.intValue()) {
                    logger.warn(msg);
                } else {
                    logger.info(msg);
                }
            }
        } else {
            if (levelValue >= Level.FINE.intValue()) {
                logger.debug(msg);
            } else {
                logger.trace(msg);
            }
        }
    }

    private void doLog(final Level level, final String msg, final Throwable throwable) {
        int levelValue = level.intValue();
        if (levelValue >= Level.INFO.intValue()) {
            if (levelValue >= Level.SEVERE.intValue()) {
                if (levelValue != Level.OFF.intValue()) {
                    logger.error(msg, throwable);
                }
            } else {
                if (levelValue >= Level.WARNING.intValue()) {
                    logger.warn(msg, throwable);
                } else {
                    logger.info(msg, throwable);
                }
            }
        } else {
            if (levelValue >= Level.FINE.intValue()) {
                logger.debug(msg, throwable);
            } else {
                logger.trace(msg, throwable);
            }
        }
    }

    public void log(final LogRecord record) {
        Throwable thrown = record.getThrown();
        Level level = record.getLevel();
        String message = record.getMessage();
        if (thrown == null) {
            doLog(level, message);
        } else {
            doLog(level, message, thrown);
        }
    }

    public void log(final Level level, final String msg) {
        doLog(level, msg);
    }

    public void log(final Level level, final String msg, final Object param1) {
        doLog(level, msg);
    }

    public void log(final Level level, final String msg, final Object[] params) {
        doLog(level, msg);
    }

    public void log(final Level level, final String msg, final Throwable thrown) {
        doLog(level, msg, thrown);
    }

    public void logp(final Level level, final String sourceClass, final String sourceMethod, final String msg) {
        doLog(level, msg);
    }

    public void logp(final Level level, final String sourceClass, final String sourceMethod, final String msg, final Object param1) {
        doLog(level, msg);
    }

    public void logp(final Level level, final String sourceClass, final String sourceMethod, final String msg, final Object[] params) {
        doLog(level, msg);
    }

    public void logp(final Level level, final String sourceClass, final String sourceMethod, final String msg, final Throwable thrown) {
        doLog(level, msg, thrown);
    }

    public void logrb(final Level level, final String sourceClass, final String sourceMethod, final String bundleName, final String msg) {
        doLog(level, msg);
    }

    public void logrb(final Level level, final String sourceClass, final String sourceMethod, final String bundleName, final String msg, final Object param1) {
        doLog(level, msg);
    }

    public void logrb(final Level level, final String sourceClass, final String sourceMethod, final String bundleName, final String msg, final Object[] params) {
        doLog(level, msg);
    }

    public void logrb(final Level level, final String sourceClass, final String sourceMethod, final String bundleName, final String msg, final Throwable thrown) {
        doLog(level, msg, thrown);
    }

    public void entering(final String sourceClass, final String sourceMethod) {
        logger.trace("ENTRY {}.{}", sourceClass, sourceMethod);
    }

    public void entering(final String sourceClass, final String sourceMethod, final Object param1) {
        if (logger.isTraceEnabled()) {
            logger.trace("ENTRY {}.{} : arg {}", new Object[] {sourceClass, sourceMethod, param1});
        }
    }

    public void entering(final String sourceClass, final String sourceMethod, final Object[] params) {
        if (logger.isTraceEnabled()) {
            StringBuilder sb = new StringBuilder("ENTRY {}.{} : ");
            if (params.length == 0) {
                sb.append("without args");
            } else if (params.length == 1) {
                sb.append("with arg {}");
            } else {
                sb.append("with args {}");
            }
            for (int i = 1; i < params.length; i++) {
                sb.append(", {}");
            }
            Object[] objects = new Object[params.length + 2];
            objects[0] = sourceClass;
            objects[1] = sourceMethod;
            System.arraycopy(params, 0, objects, 2, params.length);
            logger.trace(sb.toString(), objects);
        }
    }

    public void exiting(final String sourceClass, final String sourceMethod) {
        logger.trace("RETURN {}.{}", sourceClass, sourceMethod);
    }

    public void exiting(final String sourceClass, final String sourceMethod, final Object result) {
        if (logger.isTraceEnabled()) {
            logger.trace("RETURN {}.{} : value {}", new Object[] {sourceClass, sourceMethod, result});
        }
    }

    public void throwing(final String sourceClass, final String sourceMethod, final Throwable thrown) {
        if (logger.isTraceEnabled()) {
            logger.trace("THROW " + sourceClass + "." + sourceMethod, thrown);
        }
    }

    public void severe(final String msg) {
        logger.error(msg);
    }

    public void warning(final String msg) {
        logger.warn(msg);
    }

    public void info(final String msg) {
        logger.info(msg);
    }

    public void config(final String msg) {
        logger.debug(msg);
    }

    public void fine(final String msg) {
        logger.debug(msg);
    }

    public void finer(final String msg) {
        logger.trace(msg);
    }

    public void finest(final String msg) {
        logger.trace(msg);
    }

    public Level getLevel() {
        if (!logger.isWarnEnabled()) {
            if (!logger.isErrorEnabled()) {
                return Level.OFF;
            }
            return Level.SEVERE;
        } else {
            if (!logger.isDebugEnabled()) {
                if (!logger.isInfoEnabled()) {
                    return Level.WARNING;
                }
                return Level.INFO;
            } else {
                if (!logger.isTraceEnabled()) {
                    return Level.FINE;
                }
                return Level.ALL;
            }
        }
    }

    public boolean isLoggable(final Level level) {
        int levelValue = level.intValue();
        if (levelValue >= Level.INFO.intValue()) {
            if (levelValue >= Level.SEVERE.intValue()) {
                if (levelValue == Level.OFF.intValue()) {
                    return false;
                }
                return logger.isErrorEnabled();
            } else {
                if (levelValue >= Level.WARNING.intValue()) {
                    return logger.isWarnEnabled();
                }
                return logger.isInfoEnabled();
            }
        } else {
            if (levelValue >= Level.FINE.intValue()) {
                return logger.isDebugEnabled();
            }
            return logger.isTraceEnabled();
        }
    }

}
