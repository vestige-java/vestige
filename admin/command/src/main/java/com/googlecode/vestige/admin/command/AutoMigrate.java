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

package com.googlecode.vestige.admin.command;

import java.io.PrintWriter;
import java.util.Collections;
import java.util.List;

import com.googlecode.vestige.admin.command.argument.Argument;
import com.googlecode.vestige.application.ApplicationException;
import com.googlecode.vestige.application.ApplicationManager;

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

    public void execute(final PrintWriter out) {
        try {
            applicationManager.autoMigrate();
        } catch (ApplicationException e) {
            e.printStackTrace(out);
        }
    }


}
