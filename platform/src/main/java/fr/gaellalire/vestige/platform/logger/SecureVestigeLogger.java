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

package fr.gaellalire.vestige.platform.logger;

import java.security.PrivilegedAction;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import fr.gaellalire.vestige.core.logger.VestigeLogger;
import fr.gaellalire.vestige.platform.system.PublicVestigeSystem;

/**
 * @author Gael Lalire
 */
public class SecureVestigeLogger implements VestigeLogger {

    private PublicVestigeSystem privilegedVestigeSystem;

    private VestigeLogger vestigeLogger;

    public SecureVestigeLogger(final PublicVestigeSystem privilegedVestigeSystem, final VestigeLogger vestigeLogger) {
        this.privilegedVestigeSystem = privilegedVestigeSystem;
        this.vestigeLogger = vestigeLogger;
    }

    @Override
    public void log(final LogRecord record) {
        privilegedVestigeSystem.doPrivileged(new PrivilegedAction<Void>() {
            @Override
            public Void run() {
                vestigeLogger.log(record);
                return null;
            }
        });
    }

    @Override
    public void log(final Level level, final String msg) {
        privilegedVestigeSystem.doPrivileged(new PrivilegedAction<Void>() {
            @Override
            public Void run() {
                vestigeLogger.log(level, msg);
                return null;
            }
        });
    }

    @Override
    public void log(final Level level, final String msg, final Object param1) {
        privilegedVestigeSystem.doPrivileged(new PrivilegedAction<Void>() {
            @Override
            public Void run() {
                vestigeLogger.log(level, msg, param1);
                return null;
            }
        });
    }

    @Override
    public void log(final Level level, final String msg, final Object[] params) {
        privilegedVestigeSystem.doPrivileged(new PrivilegedAction<Void>() {
            @Override
            public Void run() {
                vestigeLogger.log(level, msg, params);
                return null;
            }
        });
    }

    @Override
    public void log(final Level level, final String msg, final Throwable thrown) {
        privilegedVestigeSystem.doPrivileged(new PrivilegedAction<Void>() {
            @Override
            public Void run() {
                vestigeLogger.log(level, msg, thrown);
                return null;
            }
        });
    }

    @Override
    public void logp(final Level level, final String sourceClass, final String sourceMethod, final String msg) {
        privilegedVestigeSystem.doPrivileged(new PrivilegedAction<Void>() {
            @Override
            public Void run() {
                vestigeLogger.logp(level, sourceClass, sourceMethod, msg);
                return null;
            }
        });
    }

    @Override
    public void logp(final Level level, final String sourceClass, final String sourceMethod, final String msg, final Object param1) {
        privilegedVestigeSystem.doPrivileged(new PrivilegedAction<Void>() {
            @Override
            public Void run() {
                vestigeLogger.logp(level, sourceClass, sourceMethod, msg, param1);
                return null;
            }
        });
    }

    @Override
    public void logp(final Level level, final String sourceClass, final String sourceMethod, final String msg, final Object[] params) {
        privilegedVestigeSystem.doPrivileged(new PrivilegedAction<Void>() {
            @Override
            public Void run() {
                vestigeLogger.logp(level, sourceClass, sourceMethod, msg, params);
                return null;
            }
        });
    }

    @Override
    public void logp(final Level level, final String sourceClass, final String sourceMethod, final String msg, final Throwable thrown) {
        privilegedVestigeSystem.doPrivileged(new PrivilegedAction<Void>() {
            @Override
            public Void run() {
                vestigeLogger.logp(level, sourceClass, sourceMethod, msg, thrown);
                return null;
            }
        });
    }

    @Override
    public void logrb(final Level level, final String sourceClass, final String sourceMethod, final String bundleName, final String msg) {
        privilegedVestigeSystem.doPrivileged(new PrivilegedAction<Void>() {
            @Override
            public Void run() {
                vestigeLogger.logrb(level, sourceClass, sourceMethod, bundleName, msg);
                return null;
            }
        });
    }

    @Override
    public void logrb(final Level level, final String sourceClass, final String sourceMethod, final String bundleName, final String msg, final Object param1) {
        privilegedVestigeSystem.doPrivileged(new PrivilegedAction<Void>() {
            @Override
            public Void run() {
                vestigeLogger.logrb(level, sourceClass, sourceMethod, bundleName, msg, param1);
                return null;
            }
        });
    }

    @Override
    public void logrb(final Level level, final String sourceClass, final String sourceMethod, final String bundleName, final String msg, final Object[] params) {
        privilegedVestigeSystem.doPrivileged(new PrivilegedAction<Void>() {
            @Override
            public Void run() {
                vestigeLogger.logrb(level, sourceClass, sourceMethod, bundleName, msg, params);
                return null;
            }
        });
    }

    @Override
    public void logrb(final Level level, final String sourceClass, final String sourceMethod, final String bundleName, final String msg, final Throwable thrown) {
        privilegedVestigeSystem.doPrivileged(new PrivilegedAction<Void>() {
            @Override
            public Void run() {
                vestigeLogger.logrb(level, sourceClass, sourceMethod, bundleName, msg, thrown);
                return null;
            }
        });
    }

    @Override
    public void entering(final String sourceClass, final String sourceMethod) {
        privilegedVestigeSystem.doPrivileged(new PrivilegedAction<Void>() {
            @Override
            public Void run() {
                vestigeLogger.entering(sourceClass, sourceMethod);
                return null;
            }
        });
    }

    @Override
    public void entering(final String sourceClass, final String sourceMethod, final Object param1) {
        privilegedVestigeSystem.doPrivileged(new PrivilegedAction<Void>() {
            @Override
            public Void run() {
                vestigeLogger.entering(sourceClass, sourceMethod, param1);
                return null;
            }
        });
    }

    @Override
    public void entering(final String sourceClass, final String sourceMethod, final Object[] params) {
        privilegedVestigeSystem.doPrivileged(new PrivilegedAction<Void>() {
            @Override
            public Void run() {
                vestigeLogger.entering(sourceClass, sourceMethod, params);
                return null;
            }
        });
    }

    @Override
    public void exiting(final String sourceClass, final String sourceMethod) {
        privilegedVestigeSystem.doPrivileged(new PrivilegedAction<Void>() {
            @Override
            public Void run() {
                vestigeLogger.exiting(sourceClass, sourceMethod);
                return null;
            }
        });
    }

    @Override
    public void exiting(final String sourceClass, final String sourceMethod, final Object result) {
        privilegedVestigeSystem.doPrivileged(new PrivilegedAction<Void>() {
            @Override
            public Void run() {
                vestigeLogger.exiting(sourceClass, sourceMethod, result);
                return null;
            }
        });
    }

    @Override
    public void throwing(final String sourceClass, final String sourceMethod, final Throwable thrown) {
        privilegedVestigeSystem.doPrivileged(new PrivilegedAction<Void>() {
            @Override
            public Void run() {
                vestigeLogger.throwing(sourceClass, sourceMethod, thrown);
                return null;
            }
        });
    }

    @Override
    public void severe(final String msg) {
        privilegedVestigeSystem.doPrivileged(new PrivilegedAction<Void>() {
            @Override
            public Void run() {
                vestigeLogger.severe(msg);
                return null;
            }
        });
    }

    @Override
    public void warning(final String msg) {
        privilegedVestigeSystem.doPrivileged(new PrivilegedAction<Void>() {
            @Override
            public Void run() {
                vestigeLogger.warning(msg);
                return null;
            }
        });
    }

    @Override
    public void info(final String msg) {
        privilegedVestigeSystem.doPrivileged(new PrivilegedAction<Void>() {
            @Override
            public Void run() {
                vestigeLogger.info(msg);
                return null;
            }
        });
    }

    @Override
    public void config(final String msg) {
        privilegedVestigeSystem.doPrivileged(new PrivilegedAction<Void>() {
            @Override
            public Void run() {
                vestigeLogger.config(msg);
                return null;
            }
        });
    }

    @Override
    public void fine(final String msg) {
        privilegedVestigeSystem.doPrivileged(new PrivilegedAction<Void>() {
            @Override
            public Void run() {
                vestigeLogger.fine(msg);
                return null;
            }
        });
    }

    @Override
    public void finer(final String msg) {
        privilegedVestigeSystem.doPrivileged(new PrivilegedAction<Void>() {
            @Override
            public Void run() {
                vestigeLogger.finer(msg);
                return null;
            }
        });
    }

    @Override
    public void finest(final String msg) {
        privilegedVestigeSystem.doPrivileged(new PrivilegedAction<Void>() {
            @Override
            public Void run() {
                vestigeLogger.finest(msg);
                return null;
            }
        });
    }

    @Override
    public Level getLevel() {
        return privilegedVestigeSystem.doPrivileged(new PrivilegedAction<Level>() {
            @Override
            public Level run() {
                return vestigeLogger.getLevel();
            }
        });
    }

    @Override
    public boolean isLoggable(final Level level) {
        return privilegedVestigeSystem.doPrivileged(new PrivilegedAction<Boolean>() {
            @Override
            public Boolean run() {
                return vestigeLogger.isLoggable(level);
            }
        });
    }

}
