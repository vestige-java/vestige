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

/**
 * @author Gael Lalire
 */
public class SLF4JOutputStream extends OutputStream {

    private static final Logger LOGGER = LoggerFactory.getLogger(SLF4JOutputStream.class);

    private VestigeSystem privilegedVestigeSystem;

    private ByteArrayOutputStream outputStream;

    private boolean crRead = false;

    private boolean info;

    public SLF4JOutputStream(final VestigeSystem privilegedVestigeSystem, final boolean info) {
        this.privilegedVestigeSystem = privilegedVestigeSystem;
        this.info = info;
        outputStream = new ByteArrayOutputStream();
    }

    @Override
    public void write(final int b) throws IOException {
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
        if (lineReaded) {
            if (info) {
                VestigeSystem pushedVestigeSystem = privilegedVestigeSystem.setCurrentSystem();
                try {
                    if (LOGGER.isInfoEnabled()) {
                        LOGGER.info("{}", outputStream.toString());
                    }
                } finally {
                    pushedVestigeSystem.setCurrentSystem();
                }
            } else {
                VestigeSystem pushedVestigeSystem = privilegedVestigeSystem.setCurrentSystem();
                try {
                    if (LOGGER.isErrorEnabled()) {
                        LOGGER.error("{}", outputStream.toString());
                    }
                } finally {
                    pushedVestigeSystem.setCurrentSystem();
                }
            }
            outputStream.reset();
        }
    }
}
