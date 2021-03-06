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
import java.util.Set;

import fr.gaellalire.vestige.spi.resolver.ResolvedClassLoaderConfiguration;

/**
 * @author Gael Lalire
 */
public interface ApplicationManagerState {

    Set<String> getRepositoriesName() throws ApplicationException;

    URL getRepositoryURL(String repoName);

    Set<String> getApplicationsName() throws ApplicationException;

    boolean isStarted(String installName) throws ApplicationException;

    ResolvedClassLoaderConfiguration getClassLoaders(String installName) throws ApplicationException;

    List<Integer> getMigrationRepositoryApplicationVersion(String installName) throws ApplicationException;

    int getAutoMigrateLevel(String installName) throws ApplicationException;

    boolean isAutoStarted(String installName) throws ApplicationException;

    URL getApplicationRepositoryURL(String installName) throws ApplicationException;

    String getRepositoryApplicationName(String installName) throws ApplicationException;

    List<Integer> getRepositoryApplicationVersion(String installName) throws ApplicationException;

    String getExceptionStackTrace(String installName) throws ApplicationException;

}
