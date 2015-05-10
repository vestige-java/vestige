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
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.googlecode.vestige.application.ApplicationException;
import com.googlecode.vestige.application.ApplicationManager;
import com.googlecode.vestige.application.VersionUtils;

/**
 * @author Gael Lalire
 */
public class VersionArgument implements Argument {

    private static final String NAME = "<version>";

    private String name;

    private Boolean installed;

    private ApplicationManager applicationManager;

    private List<Integer> version;

    private RepositoryArgument repositoryArgument;

    private ApplicationArgument applicationArgument;

    public VersionArgument(final ApplicationManager applicationManager, final Boolean installed,
            final RepositoryArgument repositoryArgument, final ApplicationArgument applicationArgument) {
        this(NAME, applicationManager, installed, repositoryArgument, applicationArgument);
    }

    public VersionArgument(final String name, final ApplicationManager applicationManager, final Boolean installed,
            final RepositoryArgument repositoryArgument, final ApplicationArgument applicationArgument) {
        this.name = name;
        this.applicationManager = applicationManager;
        this.installed = installed;
        this.repositoryArgument = repositoryArgument;
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
        if (installed != null) {
            boolean contains;
            try {
                contains = applicationManager.getVersions(repositoryArgument.getRepository(),
                        applicationArgument.getApplication()).contains(version);
            } catch (ApplicationException e) {
                throw new ParseException(e);
            }
            if (installed) {
                if (!contains) {
                    throw new ParseException(s + " is not an installed version");
                }
            } else {
                if (contains) {
                    throw new ParseException(s + " is an already installed version");
                }
            }
        }
        this.version = version;
    }

    public Collection<String> propose() throws ParseException {
        if (installed != null && installed) {
            try {
                Set<List<Integer>> versions = applicationManager.getVersions(repositoryArgument.getRepository(),
                        applicationArgument.getApplication());
                Set<String> set = new TreeSet<String>();
                for (List<Integer> version : versions) {
                    set.add(VersionUtils.toString(version));
                }
                return set;
            } catch (ApplicationException e) {
                throw new ParseException(e);
            }
        } else {
            return null;
        }
    }

    public void reset() {
        version = null;
    }

}
