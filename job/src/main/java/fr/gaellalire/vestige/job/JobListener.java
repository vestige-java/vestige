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

package fr.gaellalire.vestige.job;

/**
 *
 * @author Gael Lalire
 */
public interface JobListener {

    /**
     * This method shall work without any permission.
     * This method shall not be interruption ({@link Thread#interrupt()}) sensitive.
     */
    TaskListener taskAdded(String description);

    /**
     * When this method is called {@link JobController#isDone()} must return true.
     */
    void jobDone();

}
