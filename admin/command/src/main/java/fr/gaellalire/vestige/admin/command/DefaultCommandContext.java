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

package fr.gaellalire.vestige.admin.command;

import java.io.PrintWriter;

import fr.gaellalire.vestige.job.JobListener;

/**
 * @author Gael Lalire
 */
public class DefaultCommandContext implements CommandContext {

    private PrintWriter out;

    private JobListener jobListener;

    @Override
    public PrintWriter getOut() {
        return out;
    }

    public void setOut(final PrintWriter out) {
        this.out = out;
    }

    @Override
    public JobListener getJobListener() {
        return jobListener;
    }

    public void setJobListener(final JobListener jobListener) {
        this.jobListener = jobListener;
    }

}
