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
import fr.gaellalire.vestige.job.JobController;
import fr.gaellalire.vestige.platform.VestigePlatform;

/**
 * @author Gael Lalire
 */
public class DiscardUnattached implements Command {

    private VestigePlatform vestigePlatform;

    public DiscardUnattached(final VestigePlatform vestigePlatform) {
        this.vestigePlatform = vestigePlatform;
    }

    @Override
    public String getName() {
        return "discard-unattached";
    }

    @Override
    public List<? extends Argument> getArguments() {
        return Collections.emptyList();
    }

    @Override
    public String getDesc() {
        return "Remove unattached classloader from platform allowing to load new version of leaked classloader";
    }

    @Override
    public JobController execute(final CommandContext commandContext) {
        vestigePlatform.discardUnattached();
        return null;
    }

}
