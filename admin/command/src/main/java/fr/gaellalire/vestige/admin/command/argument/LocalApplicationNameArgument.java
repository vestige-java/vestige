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

import fr.gaellalire.vestige.application.manager.ApplicationException;
import fr.gaellalire.vestige.application.manager.ApplicationManager;

/**
 * @author Gael Lalire
 */
public class LocalApplicationNameArgument implements Argument {

    private static final String NAME = "<local-application-name>";

    public String getName() {
        return NAME;
    }

    private ApplicationManager applicationManager;

    private String application;

    private boolean used;

    public LocalApplicationNameArgument(final ApplicationManager applicationManager) {
        this(applicationManager, true);
    }

    public LocalApplicationNameArgument(final ApplicationManager applicationManager, final boolean used) {
        this.applicationManager = applicationManager;
        this.used = used;
    }

    public String getApplication() {
        return application;
    }

    public void parse(final String s) throws ParseException {
        boolean contains;
        try {
            contains = applicationManager.getApplicationsName().contains(s);
        } catch (ApplicationException e) {
            throw new ParseException(e);
        }
        if (used) {
            if (!contains) {
                throw new ParseException(s + " is not an installed application");
            }
        } else {
            if (contains) {
                throw new ParseException(s + " is an installed application");
            }
        }
        application = s;
    }

    public Collection<String> propose() throws ParseException {
        if (used) {
            try {
                return applicationManager.getApplicationsName();
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
