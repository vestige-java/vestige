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

import java.util.logging.Level;
import java.util.logging.LogRecord;

import fr.gaellalire.vestige.core.logger.VestigeLogger;
import fr.gaellalire.vestige.spi.system.VestigeSystem;

/**
 * @author Gael Lalire
 */
public class SecureVestigeLogger implements VestigeLogger {

    private VestigeSystem privilegedVestigeSystem;

    private VestigeLogger vestigeLogger;

    public SecureVestigeLogger(final VestigeSystem privilegedVestigeSystem, final VestigeLogger vestigeLogger) {
        this.privilegedVestigeSystem = privilegedVestigeSystem;
        this.vestigeLogger = vestigeLogger;
    }

    @Override
    public void log(final LogRecord record) {
        VestigeSystem pushedVestigeSystem = privilegedVestigeSystem.setCurrentSystem();
        try {
            vestigeLogger.log(record);
        } finally {
            pushedVestigeSystem.setCurrentSystem();
        }
    }

    @Override
    public void log(final Level level, final String msg) {
        VestigeSystem pushedVestigeSystem = privilegedVestigeSystem.setCurrentSystem();
        try {
            vestigeLogger.log(level, msg);
        } finally {
            pushedVestigeSystem.setCurrentSystem();
        }
    }

    @Override
    public void log(final Level level, final String msg, final Object param1) {
        VestigeSystem pushedVestigeSystem = privilegedVestigeSystem.setCurrentSystem();
        try {
            vestigeLogger.log(level, msg, param1);
        } finally {
            pushedVestigeSystem.setCurrentSystem();
        }
    }

    @Override
    public void log(final Level level, final String msg, final Object[] params) {
        VestigeSystem pushedVestigeSystem = privilegedVestigeSystem.setCurrentSystem();
        try {
            vestigeLogger.log(level, msg, params);
        } finally {
            pushedVestigeSystem.setCurrentSystem();
        }
    }

    @Override
    public void log(final Level level, final String msg, final Throwable thrown) {
        VestigeSystem pushedVestigeSystem = privilegedVestigeSystem.setCurrentSystem();
        try {
            vestigeLogger.log(level, msg, thrown);
        } finally {
            pushedVestigeSystem.setCurrentSystem();
        }
    }

    @Override
    public void logp(final Level level, final String sourceClass, final String sourceMethod, final String msg) {
        VestigeSystem pushedVestigeSystem = privilegedVestigeSystem.setCurrentSystem();
        try {
            vestigeLogger.logp(level, sourceClass, sourceMethod, msg);
        } finally {
            pushedVestigeSystem.setCurrentSystem();
        }
    }

    @Override
    public void logp(final Level level, final String sourceClass, final String sourceMethod, final String msg, final Object param1) {
        VestigeSystem pushedVestigeSystem = privilegedVestigeSystem.setCurrentSystem();
        try {
            vestigeLogger.logp(level, sourceClass, sourceMethod, msg, param1);
        } finally {
            pushedVestigeSystem.setCurrentSystem();
        }
    }

    @Override
    public void logp(final Level level, final String sourceClass, final String sourceMethod, final String msg, final Object[] params) {
        VestigeSystem pushedVestigeSystem = privilegedVestigeSystem.setCurrentSystem();
        try {
            vestigeLogger.logp(level, sourceClass, sourceMethod, msg, params);
        } finally {
            pushedVestigeSystem.setCurrentSystem();
        }
    }

    @Override
    public void logp(final Level level, final String sourceClass, final String sourceMethod, final String msg, final Throwable thrown) {
        VestigeSystem pushedVestigeSystem = privilegedVestigeSystem.setCurrentSystem();
        try {
            vestigeLogger.logp(level, sourceClass, sourceMethod, msg, thrown);
        } finally {
            pushedVestigeSystem.setCurrentSystem();
        }
    }

    @Override
    public void logrb(final Level level, final String sourceClass, final String sourceMethod, final String bundleName, final String msg) {
        VestigeSystem pushedVestigeSystem = privilegedVestigeSystem.setCurrentSystem();
        try {
            vestigeLogger.logrb(level, sourceClass, sourceMethod, bundleName, msg);
        } finally {
            pushedVestigeSystem.setCurrentSystem();
        }
    }

    @Override
    public void logrb(final Level level, final String sourceClass, final String sourceMethod, final String bundleName, final String msg, final Object param1) {
        VestigeSystem pushedVestigeSystem = privilegedVestigeSystem.setCurrentSystem();
        try {
            vestigeLogger.logrb(level, sourceClass, sourceMethod, bundleName, msg, param1);
        } finally {
            pushedVestigeSystem.setCurrentSystem();
        }
    }

    @Override
    public void logrb(final Level level, final String sourceClass, final String sourceMethod, final String bundleName, final String msg, final Object[] params) {
        VestigeSystem pushedVestigeSystem = privilegedVestigeSystem.setCurrentSystem();
        try {
            vestigeLogger.logrb(level, sourceClass, sourceMethod, bundleName, msg, params);
        } finally {
            pushedVestigeSystem.setCurrentSystem();
        }
    }

    @Override
    public void logrb(final Level level, final String sourceClass, final String sourceMethod, final String bundleName, final String msg, final Throwable thrown) {
        VestigeSystem pushedVestigeSystem = privilegedVestigeSystem.setCurrentSystem();
        try {
            vestigeLogger.logrb(level, sourceClass, sourceMethod, bundleName, msg, thrown);
        } finally {
            pushedVestigeSystem.setCurrentSystem();
        }
    }

    @Override
    public void entering(final String sourceClass, final String sourceMethod) {
        VestigeSystem pushedVestigeSystem = privilegedVestigeSystem.setCurrentSystem();
        try {
            vestigeLogger.entering(sourceClass, sourceMethod);
        } finally {
            pushedVestigeSystem.setCurrentSystem();
        }
    }

    @Override
    public void entering(final String sourceClass, final String sourceMethod, final Object param1) {
        VestigeSystem pushedVestigeSystem = privilegedVestigeSystem.setCurrentSystem();
        try {
            vestigeLogger.entering(sourceClass, sourceMethod, param1);
        } finally {
            pushedVestigeSystem.setCurrentSystem();
        }
    }

    @Override
    public void entering(final String sourceClass, final String sourceMethod, final Object[] params) {
        VestigeSystem pushedVestigeSystem = privilegedVestigeSystem.setCurrentSystem();
        try {
            vestigeLogger.entering(sourceClass, sourceMethod, params);
        } finally {
            pushedVestigeSystem.setCurrentSystem();
        }
    }

    @Override
    public void exiting(final String sourceClass, final String sourceMethod) {
        VestigeSystem pushedVestigeSystem = privilegedVestigeSystem.setCurrentSystem();
        try {
            vestigeLogger.exiting(sourceClass, sourceMethod);
        } finally {
            pushedVestigeSystem.setCurrentSystem();
        }
    }

    @Override
    public void exiting(final String sourceClass, final String sourceMethod, final Object result) {
        VestigeSystem pushedVestigeSystem = privilegedVestigeSystem.setCurrentSystem();
        try {
            vestigeLogger.exiting(sourceClass, sourceMethod, result);
        } finally {
            pushedVestigeSystem.setCurrentSystem();
        }
    }

    @Override
    public void throwing(final String sourceClass, final String sourceMethod, final Throwable thrown) {
        VestigeSystem pushedVestigeSystem = privilegedVestigeSystem.setCurrentSystem();
        try {
            vestigeLogger.throwing(sourceClass, sourceMethod, thrown);
        } finally {
            pushedVestigeSystem.setCurrentSystem();
        }
    }

    @Override
    public void severe(final String msg) {
        VestigeSystem pushedVestigeSystem = privilegedVestigeSystem.setCurrentSystem();
        try {
            vestigeLogger.severe(msg);
        } finally {
            pushedVestigeSystem.setCurrentSystem();
        }
    }

    @Override
    public void warning(final String msg) {
        VestigeSystem pushedVestigeSystem = privilegedVestigeSystem.setCurrentSystem();
        try {
            vestigeLogger.warning(msg);
        } finally {
            pushedVestigeSystem.setCurrentSystem();
        }
    }

    @Override
    public void info(final String msg) {
        VestigeSystem pushedVestigeSystem = privilegedVestigeSystem.setCurrentSystem();
        try {
            vestigeLogger.info(msg);
        } finally {
            pushedVestigeSystem.setCurrentSystem();
        }
    }

    @Override
    public void config(final String msg) {
        VestigeSystem pushedVestigeSystem = privilegedVestigeSystem.setCurrentSystem();
        try {
            vestigeLogger.config(msg);
        } finally {
            pushedVestigeSystem.setCurrentSystem();
        }
    }

    @Override
    public void fine(final String msg) {
        VestigeSystem pushedVestigeSystem = privilegedVestigeSystem.setCurrentSystem();
        try {
            vestigeLogger.fine(msg);
        } finally {
            pushedVestigeSystem.setCurrentSystem();
        }
    }

    @Override
    public void finer(final String msg) {
        VestigeSystem pushedVestigeSystem = privilegedVestigeSystem.setCurrentSystem();
        try {
            vestigeLogger.finer(msg);
        } finally {
            pushedVestigeSystem.setCurrentSystem();
        }
    }

    @Override
    public void finest(final String msg) {
        VestigeSystem pushedVestigeSystem = privilegedVestigeSystem.setCurrentSystem();
        try {
            vestigeLogger.finest(msg);
        } finally {
            pushedVestigeSystem.setCurrentSystem();
        }
    }

    @Override
    public Level getLevel() {
        VestigeSystem pushedVestigeSystem = privilegedVestigeSystem.setCurrentSystem();
        try {
            return vestigeLogger.getLevel();
        } finally {
            pushedVestigeSystem.setCurrentSystem();
        }
    }

    @Override
    public boolean isLoggable(final Level level) {
        VestigeSystem pushedVestigeSystem = privilegedVestigeSystem.setCurrentSystem();
        try {
            return vestigeLogger.isLoggable(level);
        } finally {
            pushedVestigeSystem.setCurrentSystem();
        }
    }

}
