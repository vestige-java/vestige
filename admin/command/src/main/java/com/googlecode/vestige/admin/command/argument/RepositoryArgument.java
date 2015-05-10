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
import java.util.Collections;

import com.googlecode.vestige.application.ApplicationException;
import com.googlecode.vestige.application.ApplicationManager;

/**
 * @author Gael Lalire
 */
public class RepositoryArgument implements Argument {

    private static final String NAME = "<repository>";

    public String getName() {
        return NAME;
    }

    private Boolean installed;

    private ApplicationManager applicationManager;

    private String repository;

    public RepositoryArgument(final ApplicationManager applicationManager, final Boolean installed) {
        this.applicationManager = applicationManager;
        this.installed = installed;
    }

    public String getRepository() {
        return repository;
    }

    public void parse(final String s) throws ParseException {
        if (installed != null) {
            boolean contains;
            try {
                contains = applicationManager.getRepositoriesName().contains(s);
            } catch (ApplicationException e) {
                throw new ParseException(e);
            }
            if (installed) {
                if (!contains) {
                    throw new ParseException(s + " is not an installed repo");
                }
            } else {
                if (contains) {
                    throw new ParseException(s + " is an already installed repo");
                }
            }
        }
        repository = s;
    }

    public Collection<String> propose() {
        if (installed != null && installed) {
            try {
                return applicationManager.getRepositoriesName();
            } catch (ApplicationException e) {
                return Collections.emptySet();
            }
        } else {
            return null;
        }
    }

    public void reset() {
        repository = null;
    }

}
