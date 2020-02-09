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

package fr.gaellalire.vestige.job.secure;

import fr.gaellalire.vestige.spi.job.AbstractJobHelper;
import fr.gaellalire.vestige.spi.job.JobHelper;
import fr.gaellalire.vestige.spi.job.TaskHelper;
import fr.gaellalire.vestige.spi.system.VestigeSystem;

/**
 * @author Gael Lalire
 */
public class SecureJobHelper extends AbstractJobHelper {

    private VestigeSystem secureVestigeSystem;

    private JobHelper delegate;

    public SecureJobHelper(final VestigeSystem secureVestigeSystem, final JobHelper delegate) {
        this.secureVestigeSystem = secureVestigeSystem;
        this.delegate = delegate;
    }

    @Override
    public TaskHelper addTask(final String taskDescription) {
        TaskHelper addTask;
        VestigeSystem vestigeSystem = secureVestigeSystem.setCurrentSystem();
        try {
            addTask = delegate.addTask(taskDescription);
        } finally {
            vestigeSystem.setCurrentSystem();
        }
        return new SecureTaskHelper(secureVestigeSystem, addTask);
    }

}
