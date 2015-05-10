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
import java.util.Arrays;
import java.util.List;

import com.googlecode.vestige.admin.command.argument.ApplicationArgument;
import com.googlecode.vestige.admin.command.argument.Argument;
import com.googlecode.vestige.admin.command.argument.RepositoryArgument;
import com.googlecode.vestige.admin.command.argument.VersionArgument;
import com.googlecode.vestige.application.ApplicationException;
import com.googlecode.vestige.application.ApplicationManager;

/**
 * @author Gael Lalire
 */
public class Uninstall implements Command {

    private ApplicationManager applicationManager;

    private RepositoryArgument repositoryArgument;

    private ApplicationArgument applicationArgument;

    private VersionArgument versionArgument;

    public Uninstall(final ApplicationManager applicationManager) {
        this.applicationManager = applicationManager;
        repositoryArgument = new RepositoryArgument(applicationManager, Boolean.TRUE);
        applicationArgument = new ApplicationArgument(applicationManager, Boolean.TRUE, repositoryArgument);
        versionArgument = new VersionArgument(applicationManager, Boolean.TRUE, repositoryArgument, applicationArgument);
    }

    public String getName() {
        return "uninstall";
    }

    public String getDesc() {
        return "Uninstall an application";
    }

    public List<Argument> getArguments() {
        return Arrays.asList(repositoryArgument, applicationArgument, versionArgument);
    }

    public void execute(final PrintWriter out) {
        try {
            applicationManager.uninstall(repositoryArgument.getRepository(), applicationArgument.getApplication(), versionArgument.getVersion());
        } catch (ApplicationException e) {
            e.printStackTrace(out);
        }
    }


}
