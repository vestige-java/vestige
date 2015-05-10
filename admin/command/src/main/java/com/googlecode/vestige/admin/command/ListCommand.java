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

package com.googlecode.vestige.admin.command;

import java.io.PrintWriter;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.googlecode.vestige.admin.command.argument.Argument;
import com.googlecode.vestige.application.ApplicationException;
import com.googlecode.vestige.application.ApplicationManager;
import com.googlecode.vestige.application.VersionUtils;

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
                    Set<String> applicationsName = applicationManager.getApplicationsName(repositoryName);
                    for (String applicationName : applicationsName) {
                        Set<List<Integer>> versions = applicationManager.getVersions(repositoryName, applicationName);
                        boolean first = true;
                        for (List<Integer> version : versions) {
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
                            out.print(VersionUtils.toString(version));
                            out.print(" -> state:");
                            if (applicationManager.isStarted(repositoryName, applicationName, version)) {
                                out.print("STARTED");
                            } else {
                                out.print("STOPPED");
                            }
                            out.print(", auto-migrate-level:");
                            int level = applicationManager.getAutoMigrateLevel(repositoryName, applicationName, version);
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
                    }
                    out.println("----------------------------------");
                }
            }
        } catch (ApplicationException e) {
            e.printStackTrace(out);
        }
    }

}
