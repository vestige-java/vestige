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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * @author Gael Lalire
 */
public class DefaultJobManager implements JobManager {

    private ExecutorService executorService = Executors.newCachedThreadPool();

    private Map<String, JobController> jobs = new TreeMap<String, JobController>();

    /**
     *
     * @author Gael Lalire
     */
    private static final class Result {
        private volatile boolean done;
        private volatile Exception exception;

        private Result() {
        }
    }

    @Override
    public JobController submitJob(final String jobIdPrefix, final String description, final Job action, final JobListener jobListener) {
        final List<DefaultTaskData> defaultTaskDatas = new ArrayList<DefaultTaskData>();
        final JobHelper actionHelper = new JobHelper() {

            @Override
            public TaskHelper addTask(final String taskDescription) {
                final DefaultTaskData e = new DefaultTaskData(taskDescription);
                synchronized (defaultTaskDatas) {
                    defaultTaskDatas.add(e);
                }
                final TaskListener taskListener;
                if (jobListener != null) {
                    taskListener = jobListener.taskAdded(taskDescription);
                } else {
                    taskListener = null;
                }

                return new TaskHelper() {

                    @Override
                    public void setProgress(final float progress) {
                        e.setProgress(progress);
                        if (taskListener != null) {
                            taskListener.progressChanged(progress);
                        }
                    }

                    @Override
                    public void setDone() {
                        synchronized (defaultTaskDatas) {
                            defaultTaskDatas.remove(e);
                        }
                        if (taskListener != null) {
                            taskListener.taskDone();
                        }
                    }
                };
            }
        };
        int n = 0;
        JobController actionController;
        final Result result = new Result();
        synchronized (jobs) {
            while (jobs.containsKey(jobIdPrefix + "." + n)) {
                n++;
            }
            final String jobId = jobIdPrefix + "." + n;
            final Future<?> future = executorService.submit(new Runnable() {

                @Override
                public void run() {
                    try {
                        action.run(actionHelper);
                    } catch (Exception e) {
                        result.exception = e;
                    } catch (Throwable e) {
                        result.exception = new Exception("Unexpected throwable", e);
                    } finally {
                        synchronized (jobs) {
                            jobs.remove(jobId);
                        }
                        result.done = true;
                        if (jobListener != null) {
                            jobListener.jobDone();
                        }
                    }
                }
            });
            actionController = new JobController() {

                @Override
                public void interrupt() {
                    future.cancel(true);
                }

                @Override
                public boolean isDone() {
                    return result.done;
                }

                @Override
                public String getDescription() {
                    return description;
                }

                @Override
                public List<TaskData> takeTasksSnapshot() {
                    List<TaskData> taskDatas = new ArrayList<TaskData>();
                    synchronized (defaultTaskDatas) {
                        for (DefaultTaskData defaultTaskData : defaultTaskDatas) {
                            taskDatas.add(new DefaultTaskData(defaultTaskData));
                        }
                    }
                    return taskDatas;
                }

                @Override
                public Exception getException() {
                    return result.exception;
                }

                @Override
                public String getJobId() {
                    return jobId;
                }

            };
            jobs.put(jobId, actionController);
        }
        return actionController;
    }

    @Override
    public Set<String> getJobIds() {
        return jobs.keySet();
    }

    @Override
    public JobController getJobController(final String jobId) {
        return jobs.get(jobId);
    }

}
