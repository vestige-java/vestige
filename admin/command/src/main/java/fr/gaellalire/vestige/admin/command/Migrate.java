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
import fr.gaellalire.vestige.admin.command.argument.LocalApplicationNameArgument;
import fr.gaellalire.vestige.admin.command.argument.VersionArgument;
import fr.gaellalire.vestige.application.manager.ApplicationException;
import fr.gaellalire.vestige.application.manager.ApplicationManager;
import fr.gaellalire.vestige.job.JobController;

/**
 * @author Gael Lalire
 */
public class Migrate implements Command {

    private ApplicationManager applicationManager;

    private LocalApplicationNameArgument applicationArgument;

    private VersionArgument toVersionArgument;

    public Migrate(final ApplicationManager applicationManager) {
        this.applicationManager = applicationManager;
        applicationArgument = new LocalApplicationNameArgument(applicationManager);
        toVersionArgument = new VersionArgument("<to-version>", applicationManager, applicationArgument);
    }

    public String getName() {
        return "migrate";
    }

    public String getDesc() {
        return "Migrate an application to <to-version>";
    }

    public List<Argument> getArguments() {
        return Arrays.asList(applicationArgument, toVersionArgument);
    }

    public JobController execute(final CommandContext commandContext) {
        try {
            return applicationManager.migrate(applicationArgument.getApplication(), toVersionArgument.getVersion(), commandContext.getJobListener());
        } catch (ApplicationException e) {
            e.printStackTrace(commandContext.getOut());
            return null;
        }
    }


}
