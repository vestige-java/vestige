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

import fr.gaellalire.vestige.spi.job.AbstractTaskHelper;
import fr.gaellalire.vestige.spi.job.TaskHelper;
import fr.gaellalire.vestige.spi.system.VestigeSystem;

/**
 * @author Gael Lalire
 */
public class SecureTaskHelper extends AbstractTaskHelper {

    private VestigeSystem secureVestigeSystem;

    private TaskHelper delegate;

    public SecureTaskHelper(final VestigeSystem secureVestigeSystem, final TaskHelper delegate) {
        this.secureVestigeSystem = secureVestigeSystem;
        this.delegate = delegate;
    }

    @Override
    public void setProgress(final float progress) {
        VestigeSystem vestigeSystem = secureVestigeSystem.setCurrentSystem();
        try {
            delegate.setProgress(progress);
        } finally {
            vestigeSystem.setCurrentSystem();
        }
    }

    @Override
    public void setDone() {
        VestigeSystem vestigeSystem = secureVestigeSystem.setCurrentSystem();
        try {
            delegate.setDone();
        } finally {
            vestigeSystem.setCurrentSystem();
        }
    }

    @Override
    public String toString() {
        return delegate.toString();
    }

}
