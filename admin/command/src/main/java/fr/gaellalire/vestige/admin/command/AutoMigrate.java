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

import java.util.Collections;
import java.util.List;

import fr.gaellalire.vestige.admin.command.argument.Argument;
import fr.gaellalire.vestige.application.manager.ApplicationException;
import fr.gaellalire.vestige.application.manager.ApplicationManager;
import fr.gaellalire.vestige.job.JobController;

/**
 * @author Gael Lalire
 */
public class AutoMigrate implements Command {

    private ApplicationManager applicationManager;

    public AutoMigrate(final ApplicationManager applicationManager) {
        this.applicationManager = applicationManager;
    }

    public String getName() {
        return "auto-migrate";
    }

    public String getDesc() {
        return "Check all applications and migrate them if their auto migrate level allow it";
    }

    public List<Argument> getArguments() {
        return Collections.emptyList();
    }

    public JobController execute(final CommandContext commandContext) {
        try {
            return applicationManager.autoMigrate(commandContext.getJobListener());
        } catch (ApplicationException e) {
            e.printStackTrace(commandContext.getOut());
            return null;
        }
    }


}
