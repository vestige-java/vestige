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

import java.io.PrintStream;

import fr.gaellalire.vestige.core.StackedHandler;
import fr.gaellalire.vestige.platform.system.PublicVestigeSystem;

/**
 * @author Gael Lalire
 */
public class SLF4JPrintStream extends PrintStream implements StackedHandler<PrintStream> {

    private PrintStream nextHandler;

    public SLF4JPrintStream(final PublicVestigeSystem privilegedVestigeSystem, final boolean info, final PrintStream nextHandler) {
        super(new SLF4JOutputStream(privilegedVestigeSystem, info));
        this.nextHandler = nextHandler;
    }

    public PrintStream getNextHandler() {
        return nextHandler;
    }

    public void setNextHandler(final PrintStream nextHandler) {
        this.nextHandler = nextHandler;
    }

}
