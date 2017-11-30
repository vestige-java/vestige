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

package fr.gaellalire.vestige.application.descriptor.properties;

import java.net.URL;
import java.util.List;

import fr.gaellalire.vestige.application.manager.ApplicationDescriptor;
import fr.gaellalire.vestige.application.manager.ApplicationException;
import fr.gaellalire.vestige.application.manager.ApplicationRepositoryManager;
import fr.gaellalire.vestige.application.manager.ApplicationRepositoryMetadata;
import fr.gaellalire.vestige.application.manager.CompatibilityChecker;
import fr.gaellalire.vestige.spi.job.JobHelper;

/**
 * @author Gael Lalire
 */
public class PropertiesApplicationDescriptorFactory implements ApplicationRepositoryManager {


    @Override
    public ApplicationRepositoryMetadata getMetadata(final URL context) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ApplicationDescriptor createApplicationDescriptor(final URL context, final String repoName, final String appName, final List<Integer> version, final JobHelper actionHelper)
            throws ApplicationException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean hasApplicationDescriptor(final URL context, final String repoName, final String appName, final List<Integer> version, final CompatibilityChecker compatibilityChecker)
            throws ApplicationException {
        // TODO Auto-generated method stub
        return false;
    }

}
