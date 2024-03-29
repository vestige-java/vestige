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

package fr.gaellalire.vestige.application.manager;

import java.net.URL;
import java.util.List;

import fr.gaellalire.vestige.spi.job.JobHelper;

/**
 * @author Gael Lalire
 */
public interface ApplicationRepositoryManager {

    boolean hasApplicationDescriptor(URL context, String appName, List<Integer> version, CompatibilityChecker compatibilityChecker) throws ApplicationException;

    ApplicationDescriptor createApplicationDescriptor(RepositoryOverride repositoryOverride, URL context, String appName, List<Integer> version, JobHelper jobHelper)
            throws ApplicationException;

    ApplicationRepositoryMetadata getMetadata(URL context);

}
