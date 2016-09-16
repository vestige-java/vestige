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

import java.util.Arrays;
import java.util.List;

import fr.gaellalire.vestige.admin.command.argument.Argument;
import fr.gaellalire.vestige.admin.command.argument.JobIdArgument;
import fr.gaellalire.vestige.job.JobController;
import fr.gaellalire.vestige.job.JobManager;

/**
 * @author Gael Lalire
 */
public class Kill implements Command {

    private JobManager jobManager;

    private JobIdArgument jobNameArgument;

    public Kill(final JobManager jobManager) {
        this.jobManager = jobManager;
        jobNameArgument = new JobIdArgument(jobManager);
    }

    public String getName() {
        return "kill";
    }

    public String getDesc() {
        return "Kill a job";
    }

    public List<? extends Argument> getArguments() {
        return Arrays.asList(jobNameArgument);
    }

    public JobController execute(final CommandContext commandContext) {
        try {
            JobController jobController = jobManager.getJobController(jobNameArgument.getJobId());
            if (jobController != null) {
                jobController.interrupt();
            }
        } catch (Exception e) {
            e.printStackTrace(commandContext.getOut());
        }
        return null;
    }


}
