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

import org.slf4j.Logger;

/**
 * @author Gael Lalire
 */
public class OutputStreamState {

    private ByteArrayOutputStream outputStream;

    private Logger logger;

    private boolean crRead;

    public OutputStreamState(final ByteArrayOutputStream byteArrayOutputStream, final Logger logger) {
        this.outputStream = byteArrayOutputStream;
        this.logger = logger;
    }

    public void setLogger(final Logger logger) {
        this.logger = logger;
    }

    public Logger getLogger() {
        return logger;
    }

    public void setCrRead(final boolean crRead) {
        this.crRead = crRead;
    }

    public boolean isCrRead() {
        return crRead;
    }

    public ByteArrayOutputStream getOutputStream() {
        return outputStream;
    }

}
