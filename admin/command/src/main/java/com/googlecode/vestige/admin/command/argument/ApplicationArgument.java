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

package com.googlecode.vestige.admin.command.argument;

import java.util.Collection;

import com.googlecode.vestige.application.ApplicationException;
import com.googlecode.vestige.application.ApplicationManager;

/**
 * @author Gael Lalire
 */
public class ApplicationArgument implements Argument {

    private static final String NAME = "<application>";

    public String getName() {
        return NAME;
    }

    private Boolean installed;

    private ApplicationManager applicationManager;

    private String application;

    private RepositoryArgument repositoryArgument;

    public ApplicationArgument(final ApplicationManager applicationManager, final Boolean installed,
            final RepositoryArgument repositoryArgument) {
        this.applicationManager = applicationManager;
        this.installed = installed;
        this.repositoryArgument = repositoryArgument;
    }

    public String getApplication() {
        return application;
    }

    public void parse(final String s) throws ParseException {
        if (installed != null) {
            boolean contains;
            try {
                contains = applicationManager.getApplicationsName(repositoryArgument.getRepository()).contains(s);
            } catch (ApplicationException e) {
                throw new ParseException(e);
            }
            if (installed) {
                if (!contains) {
                    throw new ParseException(s + " is not an installed application");
                }
            } else {
                if (contains) {
                    throw new ParseException(s + " is an already installed application");
                }
            }
        }
        application = s;
    }

    public Collection<String> propose() throws ParseException {
        if (installed != null && installed) {
            try {
                return applicationManager.getApplicationsName(repositoryArgument.getRepository());
            } catch (ApplicationException e) {
                throw new ParseException(e);
            }
        } else {
            return null;
        }
    }

    public void reset() {
        application = null;
    }

}
