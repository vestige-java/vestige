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
import fr.gaellalire.vestige.application.manager.VersionUtils;

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

    public void execute(final PrintWriter out) {
        try {
            synchronized (applicationManager) {
                Set<String> repositoriesName = applicationManager.getRepositoriesName();
                for (String repositoryName : repositoriesName) {
                    out.println("----------------------------------");
                    out.println("Repository : " + repositoryName + " (" + applicationManager.getRepositoryURL(repositoryName)
                            + ")");
                    out.println();
                    Set<String> applicationsName = applicationManager.getApplicationsName();
                    for (String applicationName : applicationsName) {
                        boolean first = true;
                            if (first) {
                                out.print(applicationName);
                                out.print(" ");
                                first = false;
                            } else {
                                for (int i = 0; i < applicationName.length(); i++) {
                                    out.print(" ");
                                }
                                out.print(" ");
                            }
                            out.print(applicationManager.getRepositoryName(applicationName));
                            out.print("-");
                            out.print(applicationManager.getRepositoryApplicationName(applicationName));
                            out.print("-");
                            out.print(VersionUtils.toString(applicationManager.getRepositoryApplicationVersion(applicationName)));
                            List<Integer> migrationRepositoryApplicationVersion = applicationManager.getMigrationRepositoryApplicationVersion(applicationName);
                            if (migrationRepositoryApplicationVersion != null) {
                                out.print("-");
                                out.print(VersionUtils.toString(migrationRepositoryApplicationVersion));
                            }
                            out.print(" -> state:");
                            if (applicationManager.isStarted(applicationName)) {
                                out.print("STARTED");
                            } else {
                                out.print("STOPPED");
                            }
                            out.print(", auto-start:");
                            if (applicationManager.isAutoStarted(applicationName)) {
                                out.print("ON");
                            } else {
                                out.print("OFF");
                            }
                            out.print(", auto-migrate-level:");
                            int level = applicationManager.getAutoMigrateLevel(applicationName);
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
                }
            }
        } catch (ApplicationException e) {
            e.printStackTrace(out);
        }
    }

}
