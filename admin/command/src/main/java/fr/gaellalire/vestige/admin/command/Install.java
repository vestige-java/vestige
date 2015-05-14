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

import fr.gaellalire.vestige.admin.command.argument.ApplicationArgument;
import fr.gaellalire.vestige.admin.command.argument.Argument;
import fr.gaellalire.vestige.admin.command.argument.RepositoryArgument;
import fr.gaellalire.vestige.admin.command.argument.VersionArgument;
import fr.gaellalire.vestige.application.manager.ApplicationException;
import fr.gaellalire.vestige.application.manager.ApplicationManager;

/**
 * @author Gael Lalire
 */
public class Install implements Command {

    private ApplicationManager applicationManager;

    private RepositoryArgument repositoryArgument;

    private ApplicationArgument applicationArgument;

    private VersionArgument versionArgument;

    public Install(final ApplicationManager applicationManager) {
        this.applicationManager = applicationManager;
        repositoryArgument = new RepositoryArgument(applicationManager, Boolean.TRUE);
        applicationArgument = new ApplicationArgument(applicationManager, null, repositoryArgument);
        versionArgument = new VersionArgument(applicationManager, Boolean.FALSE, repositoryArgument, applicationArgument);
    }

    public String getName() {
        return "install";
    }

    public String getDesc() {
        return "install an application";
    }

    public List<Argument> getArguments() {
        return Arrays.asList(repositoryArgument, applicationArgument, versionArgument);
    }

    public void execute(final PrintWriter out) {
        try {
            applicationManager.install(repositoryArgument.getRepository(), applicationArgument.getApplication(), versionArgument.getVersion());
        } catch (ApplicationException e) {
            e.printStackTrace(out);
        }
    }

}