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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.gaellalire.vestige.spi.system.VestigeSystem;
import fr.gaellalire.vestige.system.interceptor.VestigePrintStream;

/**
 * @author Gael Lalire
 */
public class SLF4JOutputStream extends OutputStream {

    private VestigeSystem privilegedVestigeSystem;

    private boolean info;

    private ThreadLocal<OutputStreamState> threadLocal = new ThreadLocal<OutputStreamState>();

    public SLF4JOutputStream(final VestigeSystem privilegedVestigeSystem, final boolean info) {
        this.privilegedVestigeSystem = privilegedVestigeSystem;
        this.info = info;
    }

    public String getCallingClassName() {
        boolean found = false;
        StackTraceElement[] stackTrace = new Throwable().getStackTrace();
        for (StackTraceElement stackTraceElement : stackTrace) {
            String className = stackTraceElement.getClassName();
            if (className.equals(VestigePrintStream.class.getName())) {
                found = true;
                continue;
            }
            if (found) {
                return className;
            }
        }
        if (info) {
            return "sysout";
        } else {
            return "syserr";
        }
    }

    public void log(final Logger logger, final ByteArrayOutputStream outputStream) {
        if (info) {
            VestigeSystem pushedVestigeSystem = privilegedVestigeSystem.setCurrentSystem();
            try {
                if (logger.isInfoEnabled()) {
                    logger.info("{}", outputStream.toString());
                }
            } finally {
                pushedVestigeSystem.setCurrentSystem();
            }
        } else {
            VestigeSystem pushedVestigeSystem = privilegedVestigeSystem.setCurrentSystem();
            try {
                if (logger.isErrorEnabled()) {
                    logger.error("{}", outputStream.toString());
                }
            } finally {
                pushedVestigeSystem.setCurrentSystem();
            }
        }
        outputStream.reset();
    }

    @Override
    public void write(final int b) throws IOException {
        OutputStreamState outputStreamState = threadLocal.get();
        String callingClassName = getCallingClassName();
        if (outputStreamState == null) {
            outputStreamState = new OutputStreamState(new ByteArrayOutputStream(), LoggerFactory.getLogger(callingClassName));
            threadLocal.set(outputStreamState);
        }

        boolean crRead = outputStreamState.isCrRead();
        ByteArrayOutputStream outputStream = outputStreamState.getOutputStream();
        Logger logger = outputStreamState.getLogger();

        if (!callingClassName.equals(logger.getName())) {
            log(logger, outputStream);
            logger = LoggerFactory.getLogger(callingClassName);
            crRead = false;
            outputStreamState.setLogger(logger);
        }

        boolean lineReaded = false;
        if (crRead) {
            if (b == '\r') {
                lineReaded = true;
            } else if (b == '\n') {
                // already readed
                crRead = false;
            } else {
                outputStream.write(b);
                crRead = false;
            }
        } else {
            if (b == '\r') {
                crRead = true;
                lineReaded = true;
            } else if (b == '\n') {
                lineReaded = true;
            } else {
                outputStream.write(b);
            }
        }
        outputStreamState.setCrRead(crRead);
        if (lineReaded) {
            log(logger, outputStream);
            threadLocal.remove();
        }
    }
}
