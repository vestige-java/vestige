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

package fr.gaellalire.vestige.edition.standard;

import java.io.PrintWriter;

/**
 * @author Gael Lalire
 */
public class PrintWriterVestigeStateListener implements VestigeStateListener {

    private PrintWriter printWriter;

    public PrintWriterVestigeStateListener(final PrintWriter printWriter) {
        this.printWriter = printWriter;
    }

    @Override
    public void starting() {
        printWriter.println("Starting");
    }

    @Override
    public void started() {
        printWriter.println("Started");
    }

    @Override
    public void failed() {
        printWriter.println("Failed");
    }

    @Override
    public void stopping() {
        printWriter.println("Stopping");
    }

    @Override
    public void stopped() {
        printWriter.println("Stopped");
    }

    @Override
    public void webAdminAvailable(final String url) {
        printWriter.println("Web " + url);
    }

}
