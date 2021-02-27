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

import java.io.PrintStream;

import fr.gaellalire.vestige.core.StackedHandler;
import fr.gaellalire.vestige.spi.system.VestigeSystem;

/**
 * @author Gael Lalire
 */
public class SLF4JPrintStream extends PrintStream implements StackedHandler<PrintStream> {

    private PrintStream nextHandler;

    private SLF4JOutputStream slf4jOutputStream;

    public static SLF4JOutputStream createSLF4JOutputStream(final VestigeSystem privilegedVestigeSystem, final boolean info) {
        return new SLF4JOutputStream(privilegedVestigeSystem, info);
    }

    public SLF4JPrintStream(final VestigeSystem privilegedVestigeSystem, final boolean info, final PrintStream nextHandler) {
        this(createSLF4JOutputStream(privilegedVestigeSystem, info), nextHandler);
    }

    public SLF4JPrintStream(final SLF4JOutputStream slf4jOutputStream, final PrintStream nextHandler) {
        super(slf4jOutputStream);
        this.slf4jOutputStream = slf4jOutputStream;
        this.nextHandler = nextHandler;
    }

    public PrintStream getNextHandler() {
        return nextHandler;
    }

    public void setNextHandler(final PrintStream nextHandler) {
        this.nextHandler = nextHandler;
    }

    public void cleanCurrentThreadLocal() {
        slf4jOutputStream.cleanCurrentThreadLocal();
    }

}
