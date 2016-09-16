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
public class DefaultTaskData implements TaskData {

    private String description;

    private float progress;

    public DefaultTaskData(final DefaultTaskData defaultTaskData) {
        this.description = defaultTaskData.description;
        this.progress = defaultTaskData.progress;
    }

    public DefaultTaskData(final String description) {
        this.description = description;
        this.progress = -1;
    }

    public void setProgress(final float progress) {
        this.progress = progress;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public float getProgress() {
        return progress;
    }

}
