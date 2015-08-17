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

package fr.gaellalire.vestige.admin.command.argument;

import java.util.Collection;

import fr.gaellalire.vestige.application.manager.ApplicationManager;

/**
 * @author Gael Lalire
 */
public class RepositoryApplicationNameArgument implements Argument {

    private static final String NAME = "<repository-application-name>";

    public String getName() {
        return NAME;
    }

    private RepositoryArgument repositoryArgument;

    private ApplicationManager applicationManager;

    private String application;

    public RepositoryApplicationNameArgument(final ApplicationManager applicationManager, final RepositoryArgument repositoryArgument) {
        this.applicationManager = applicationManager;
        this.repositoryArgument = repositoryArgument;
    }

    public String getApplication() {
        return application;
    }

    public void parse(final String s) throws ParseException {
        if (!applicationManager.getRepositoryApplicationsName(repositoryArgument.getRepository()).contains(s)) {
            throw new ParseException("Application not found in repository");
        }
        application = s;
    }

    public Collection<String> propose() throws ParseException {
        return applicationManager.getRepositoryApplicationsName(repositoryArgument.getRepository());
    }

    public void reset() {
        application = null;
    }

}