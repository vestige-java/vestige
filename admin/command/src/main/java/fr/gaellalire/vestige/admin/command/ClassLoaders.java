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

import fr.gaellalire.vestige.admin.command.argument.Argument;
import fr.gaellalire.vestige.admin.command.argument.LocalApplicationNameArgument;
import fr.gaellalire.vestige.application.manager.ApplicationException;
import fr.gaellalire.vestige.application.manager.ApplicationManager;
import fr.gaellalire.vestige.job.JobController;

/**
 * @author Gael Lalire
 */
public class ClassLoaders implements Command {

    private ApplicationManager applicationManager;

    private LocalApplicationNameArgument applicationArgument;

    public ClassLoaders(final ApplicationManager applicationManager) {
        this.applicationManager = applicationManager;
        applicationArgument = new LocalApplicationNameArgument(applicationManager, Boolean.TRUE);
    }

    public String getName() {
        return "classloaders";
    }

    public String getDesc() {
        return "Classloaders of an application";
    }

    public List<Argument> getArguments() {
        return Collections.<Argument> singletonList(applicationArgument);
    }

    public JobController execute(final CommandContext commandContext) {
        PrintWriter out = commandContext.getOut();
        try {
            out.println(applicationManager.getClassLoaders(applicationArgument.getApplication()));
        } catch (ApplicationException e) {
            e.printStackTrace(out);
        }
        return null;
    }

}
