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
import java.util.Arrays;
import java.util.List;

import fr.gaellalire.vestige.admin.command.argument.Argument;
import fr.gaellalire.vestige.admin.command.argument.LocalApplicationNameArgument;
import fr.gaellalire.vestige.admin.command.argument.OnOffArgument;
import fr.gaellalire.vestige.application.manager.ApplicationException;
import fr.gaellalire.vestige.application.manager.ApplicationManager;

/**
 * @author Gael Lalire
 */
public class AutoStart implements Command {

    private ApplicationManager applicationManager;

    private LocalApplicationNameArgument applicationArgument;

    private OnOffArgument onOffArgument;

    public AutoStart(final ApplicationManager applicationManager) {
        this.applicationManager = applicationManager;
        applicationArgument = new LocalApplicationNameArgument(applicationManager);
        onOffArgument = new OnOffArgument();
    }

    public String getName() {
        return "auto-start";
    }

    public String getDesc() {
        return "Set if application should start with platform";
    }

    public List<? extends Argument> getArguments() {
        return Arrays.asList(applicationArgument, onOffArgument);
    }

    public void execute(final PrintWriter out) {
        try {
            Boolean active = onOffArgument.getActive();
            if (active != null) {
                applicationManager.setAutoStarted(applicationArgument.getApplication(), active.booleanValue());
            }
        } catch (ApplicationException e) {
            e.printStackTrace(out);
        }
    }

}