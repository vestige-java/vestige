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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import fr.gaellalire.vestige.application.manager.ApplicationManager;
import fr.gaellalire.vestige.application.manager.VersionUtils;

/**
 * @author Gael Lalire
 */
public class RepositoryApplicationVersionArgument implements Argument {

    private static final String NAME = "<repository-application-version>";

    public String getName() {
        return NAME;
    }

    private RepositoryArgument repositoryArgument;

    private ApplicationManager applicationManager;

    private RepositoryApplicationNameArgument repositoryApplicationArgument;

    private List<Integer> version;

    public RepositoryApplicationVersionArgument(final ApplicationManager applicationManager, final RepositoryArgument repositoryArgument,
            final RepositoryApplicationNameArgument repositoryApplicationArgument) {
        this.applicationManager = applicationManager;
        this.repositoryArgument = repositoryArgument;
        this.repositoryApplicationArgument = repositoryApplicationArgument;
    }

    public List<Integer> getVersion() {
        return version;
    }

    public void parse(final String s) throws ParseException {
        List<Integer> fromString = VersionUtils.fromString(s);
        if (!applicationManager.getRepositoryMetadata(repositoryArgument.getRepository()).listApplicationVersions(repositoryApplicationArgument.getApplication()).contains(fromString)) {
            throw new ParseException("Application not found in repository");
        }
        version = fromString;
    }

    public Collection<String> propose() throws ParseException {
        List<String> strings = new ArrayList<String>();
        for (List<Integer> version : applicationManager.getRepositoryMetadata(repositoryArgument.getRepository()).listApplicationVersions(repositoryApplicationArgument.getApplication())) {
            strings.add(VersionUtils.toString(version));
        }
        return strings;
    }

    public void reset() {
        version = null;
    }

}
