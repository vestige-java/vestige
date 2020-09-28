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

package fr.gaellalire.vestige.application.manager;

import java.io.File;
import java.net.URL;
import java.util.List;

import fr.gaellalire.vestige.job.JobController;
import fr.gaellalire.vestige.job.JobListener;

/**
 * @author Gael Lalire
 */
public interface ApplicationManager extends ApplicationManagerState {

    ApplicationManagerState copyState() throws ApplicationException;

    void createRepository(String name, URL url) throws ApplicationException;

    void removeRepository(String name) throws ApplicationException;

    JobController install(URL overrideURL, URL repoURL, String appName, List<Integer> version, String installName, JobListener jobListener) throws ApplicationException;

    JobController reloadDescriptor(String application, JobListener jobListener) throws ApplicationException;

    JobController uninstall(String installName, JobListener jobListener) throws ApplicationException;

    JobController migrate(String installName, List<Integer> toVersion, JobListener jobListener) throws ApplicationException;

    void start(String installName) throws ApplicationException;

    JobController stop(String installName, JobListener jobListener) throws ApplicationException;

    void setAutoMigrateLevel(String installName, int level) throws ApplicationException;

    void setAutoStarted(String installName, boolean autoStarted) throws ApplicationException;

    JobController autoMigrate(JobListener jobListener) throws ApplicationException;

    JobController autoMigrate(String installName, JobListener jobListener) throws ApplicationException;

    ApplicationRepositoryMetadata getRepositoryMetadata(URL repoURL);

    void addStateListener(ApplicationManagerStateListener listener);

    void removeStateListener(ApplicationManagerStateListener listener);

    void open(File file);

    void open(URL url);

}
