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

package fr.gaellalire.vestige.admin.command.argument;

import fr.gaellalire.vestige.job.JobManager;

/**
 * @author Gael Lalire
 */
public class JobIdArgument implements Argument {

    private JobManager jobManager;

    private String jobId;

    public JobIdArgument(final JobManager jobManager) {
        this.jobManager = jobManager;
    }

    @Override
    public void parse(final String s) throws ParseException {
        jobId = s;
    }

    public void propose(final ProposeContext proposeContext) {
        for (String proposition : jobManager.getJobIds()) {
            proposeContext.addProposition(proposition);
        }
    }

    @Override
    public void reset() {
        jobId = null;
    }

    public String getJobId() {
        return jobId;
    }

    @Override
    public String getName() {
        return "<job-id>";
    }

}
