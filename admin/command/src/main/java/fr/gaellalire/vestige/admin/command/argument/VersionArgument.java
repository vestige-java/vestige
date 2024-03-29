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

import java.util.List;

import fr.gaellalire.vestige.application.manager.ApplicationException;
import fr.gaellalire.vestige.application.manager.ApplicationManager;
import fr.gaellalire.vestige.application.manager.VersionUtils;

/**
 * @author Gael Lalire
 */
public class VersionArgument implements Argument {

    private String name;

    private ApplicationManager applicationManager;

    private List<Integer> version;

    private LocalApplicationNameArgument applicationArgument;

    public VersionArgument(final String name, final ApplicationManager applicationManager, final LocalApplicationNameArgument applicationArgument) {
        this.name = name;
        this.applicationManager = applicationManager;
        this.applicationArgument = applicationArgument;
    }

    public String getName() {
        return name;
    }

    public List<Integer> getVersion() {
        return version;
    }

    public void parse(final String s) throws ParseException {
        List<Integer> version;
        try {
            version = VersionUtils.fromString(s);
        } catch (IllegalArgumentException e) {
            throw new ParseException(e);
        }
        this.version = version;
    }

    public void propose(final ProposeContext proposeContext) throws ParseException {
        try {
            String installName = applicationArgument.getApplication();
            for (List<Integer> version : applicationManager.getRepositoryMetadata(applicationManager.getRepositoryURL(installName))
                    .listApplicationVersions(applicationManager.getRepositoryApplicationName(installName))) {
                String proposition = VersionUtils.toString(version);
                proposeContext.addProposition(proposition);
            }
        } catch (ApplicationException e) {
            throw new ParseException(e);
        }
    }

    public void reset() {
        version = null;
    }

}
