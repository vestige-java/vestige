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
import fr.gaellalire.vestige.application.manager.ApplicationException;
import fr.gaellalire.vestige.application.manager.ApplicationManager;
import fr.gaellalire.vestige.application.manager.ApplicationManagerState;
import fr.gaellalire.vestige.application.manager.VersionUtils;
import fr.gaellalire.vestige.job.JobController;

/**
 * @author Gael Lalire
 */
public class ListCommand implements Command {

    private ApplicationManager applicationManager;

    public ListCommand(final ApplicationManager applicationManager) {
        this.applicationManager = applicationManager;
    }

    public String getName() {
        return "list";
    }

    public String getDesc() {
        return "list all installed applications";
    }

    public List<Argument> getArguments() {
        return Collections.emptyList();
    }

    public JobController execute(final CommandContext commandContext) {
        PrintWriter out = commandContext.getOut();
        try {
            ApplicationManagerState applicationManagerState = applicationManager.copyState();
            Set<String> repositoriesName = applicationManagerState.getRepositoriesName();
            out.println("----------------------------------");
            out.println("Repositories:");
            for (String repositoryName : repositoriesName) {
                out.print("  ");
                out.println(repositoryName + " (" + applicationManagerState.getRepositoryURL(repositoryName) + ")");
            }
            out.println("----------------------------------");
            out.println("Applications:");
            Set<String> applicationsName = applicationManagerState.getApplicationsName();
            for (String applicationName : applicationsName) {
                out.print("  ");
                out.print(applicationName);
                out.print(" (");
                out.print(applicationManagerState.getApplicationRepositoryURL(applicationName));
                String repositoryApplicationName = applicationManagerState.getRepositoryApplicationName(applicationName);
                out.print(repositoryApplicationName);
                out.print("/");
                out.print(repositoryApplicationName);
                out.print("-");
                out.print(VersionUtils.toString(applicationManagerState.getRepositoryApplicationVersion(applicationName)));
                out.print(".xml)");
                List<Integer> migrationRepositoryApplicationVersion = applicationManagerState.getMigrationRepositoryApplicationVersion(applicationName);
                if (migrationRepositoryApplicationVersion != null && migrationRepositoryApplicationVersion.size() != 0) {
                    out.print(" migrating to ");
                    out.print(VersionUtils.toString(migrationRepositoryApplicationVersion));
                }
                out.print(" -> state:");
                if (applicationManagerState.isStarted(applicationName)) {
                    out.print("STARTED");
                } else {
                    out.print("STOPPED");
                }
                out.print(", auto-start:");
                if (applicationManagerState.isAutoStarted(applicationName)) {
                    out.print("ON");
                } else {
                    out.print("OFF");
                }
                out.print(", auto-migrate-level:");
                int level = applicationManagerState.getAutoMigrateLevel(applicationName);
                out.print(level);
                switch (level) {
                case 0:
                    out.print(" (OFF)");
                    break;
                case 1:
                    out.print(" (BUG_FIXES)");
                    break;
                case 2:
                    out.print(" (BUG_FIXES and MINOR_EVOLUTIONS)");
                    break;
                case 3:
                    out.print(" (BUG_FIXES, MINOR_EVOLUTIONS and MAJOR_EVOLUTIONS)");
                    break;
                default:
                    break;
                }
                out.println();
            }
            out.println("----------------------------------");
        } catch (ApplicationException e) {
            e.printStackTrace(out);
        }
        return null;
    }

}
