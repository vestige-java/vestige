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

import java.io.File;

/**
 * @author Gael Lalire
 */
public class NoopVestigeStateListener implements VestigeStateListener {

    @Override
    public void starting() {
    }

    @Override
    public void started() {
    }

    @Override
    public void failed() {
    }

    @Override
    public void stopping() {
    }

    @Override
    public void stopped() {
    }

    @Override
    public void webAdminAvailable(final String url) {
    }

    @Override
    public void config(final File file) {
    }

    @Override
    public void certificateAuthorityGenerated(final File file) {
    }

    @Override
    public void clientP12Generated(final File file) {
    }

}
