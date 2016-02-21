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
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.List;

import fr.gaellalire.vestige.admin.command.argument.Argument;

/**
 * @author Gael Lalire
 */
public class CheckBootstrap implements Command {

    private WeakReference<Object> bootstrapObject;

    public CheckBootstrap(final WeakReference<Object> bootstrapObject) {
        this.bootstrapObject = bootstrapObject;
    }

    public String getName() {
        return "check-bootstrap";
    }

    public String getDesc() {
        return "Verify if bootstrap has been GC";
    }

    public List<Argument> getArguments() {
        return Collections.emptyList();
    }

    public void execute(final PrintWriter out) {
        if (bootstrapObject.get() != null) {
            out.println("Bootstrap is not GC");
        } else {
            out.println("Bootstrap has been GC");
        }
    }

}
