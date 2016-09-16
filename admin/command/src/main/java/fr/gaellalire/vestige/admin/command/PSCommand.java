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
import java.util.Collections;
import java.util.List;
import java.util.Set;

import fr.gaellalire.vestige.admin.command.argument.Argument;
import fr.gaellalire.vestige.job.JobController;
import fr.gaellalire.vestige.job.JobManager;
import fr.gaellalire.vestige.job.TaskData;

/**
 * @author Gael Lalire
 */
public class PSCommand implements Command {

    private JobManager jobManager;

    public PSCommand(final JobManager jobManager) {
        this.jobManager = jobManager;
    }

    @Override
    public String getName() {
        return "ps";
    }

    @Override
    public List<? extends Argument> getArguments() {
        return Collections.emptyList();
    }

    @Override
    public String getDesc() {
        return "List all jobs";
    }

    @Override
    public JobController execute(final CommandContext commandContext) {
        PrintWriter out = commandContext.getOut();
        Set<String> jobNames = jobManager.getJobIds();
        boolean one = false;
        for (String jobName : jobNames) {
            JobController jobController = jobManager.getJobController(jobName);
            if (jobController == null) {
                continue;
            }
            if (!one) {
                out.println("Job id : Job description : Current task(s)");
                one = true;
            }
            List<TaskData> takeTasksSnapshot = jobController.takeTasksSnapshot();
            int size = takeTasksSnapshot.size();
            out.print(jobName);
            out.print(" : ");
            out.print(jobController.getDescription());
            out.print(" : ");
            if (size == 0) {
                out.println("No task is running");
            } else if (size == 1) {
                TaskData taskData = takeTasksSnapshot.get(0);
                out.print(taskData.getDescription());
                float progress = taskData.getProgress();
                if (progress >= 0) {
                    out.print(" ");
                    out.print((int) (progress * 100));
                    out.println("%");
                } else {
                    out.println();
                }
            } else {
                out.println("Running " + size + " tasks");
            }
        }
        if (!one) {
            out.println("No job is running");
        }
        return null;
    }

}
