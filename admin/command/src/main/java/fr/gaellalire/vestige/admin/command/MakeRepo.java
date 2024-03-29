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
import fr.gaellalire.vestige.admin.command.argument.RepositoryArgument;
import fr.gaellalire.vestige.admin.command.argument.URLArgument;
import fr.gaellalire.vestige.application.manager.ApplicationException;
import fr.gaellalire.vestige.application.manager.ApplicationManager;
import fr.gaellalire.vestige.job.JobController;

/**
 * @author Gael Lalire
 */
public class MakeRepo implements Command {

    private ApplicationManager applicationManager;

    private RepositoryArgument repositoryArgument;

    private URLArgument urlArgument;

    public MakeRepo(final ApplicationManager applicationManager) {
        this.applicationManager = applicationManager;
        repositoryArgument = new RepositoryArgument(applicationManager, Boolean.FALSE);
        urlArgument = new URLArgument(true);
    }

    public String getName() {
        return "mk-repo";
    }

    public String getDesc() {
        return "create a new repository";
    }

    public List<Argument> getArguments() {
        return Arrays.asList(repositoryArgument, urlArgument);
    }

    public JobController execute(final CommandContext commandContext) {
        try {
            applicationManager.createRepository(repositoryArgument.getRepository(), urlArgument.getUrl());
        } catch (ApplicationException e) {
            e.printStackTrace(commandContext.getOut());
        }
        return null;
    }


}
