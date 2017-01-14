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
import java.util.Arrays;
import java.util.List;

import fr.gaellalire.vestige.admin.command.argument.Argument;
import fr.gaellalire.vestige.admin.command.argument.JobIdArgument;
import fr.gaellalire.vestige.job.JobController;
import fr.gaellalire.vestige.job.JobManager;
import fr.gaellalire.vestige.job.TaskData;

/**
 * @author Gael Lalire
 */
public class TasksCommand implements Command {

    private JobManager jobManager;

    private JobIdArgument jobNameArgument;

    public TasksCommand(final JobManager jobManager) {
        this.jobManager = jobManager;
        jobNameArgument = new JobIdArgument(jobManager);
    }

    public String getName() {
        return "tasks";
    }

    public String getDesc() {
        return "Print running tasks of a job";
    }

    public List<? extends Argument> getArguments() {
        return Arrays.asList(jobNameArgument);
    }

    public JobController execute(final CommandContext commandContext) {
        try {
            String jobId = jobNameArgument.getJobId();
            JobController jobController = jobManager.getJobController(jobId);
            PrintWriter out = commandContext.getOut();
            if (jobController != null) {
                List<TaskData> tasksSnapshot = jobController.takeTasksSnapshot();
                if (tasksSnapshot.size() == 0) {
                    out.println("No task is running");
                } else {
                    for (TaskData taskData : tasksSnapshot) {
                        int progress = (int) (taskData.getProgress() * 100);
                        out.println(taskData.getDescription() + " " + progress + "%");
                    }
                }
            } else {
                out.println("Unable to find job " + jobId);
            }
        } catch (Exception e) {
            e.printStackTrace(commandContext.getOut());
        }
        return null;
    }

}
