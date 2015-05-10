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

package com.googlecode.vestige.application;

import java.net.URL;
import java.util.List;
import java.util.Set;

import com.googlecode.vestige.platform.ClassLoaderConfiguration;

/**
 * @author Gael Lalire
 */
public interface ApplicationManager {

    Set<String> getRepositoriesName() throws ApplicationException;

    URL getRepositoryURL(final String repoName) throws ApplicationException;

    Set<String> getApplicationsName(String repo) throws ApplicationException;

    Set<List<Integer>> getVersions(String repo, String appName) throws ApplicationException;

    ClassLoaderConfiguration getClassLoaders(final String repoName, final String appName, final List<Integer> version) throws ApplicationException;

    boolean isStarted(String repoName, String appName, List<Integer> version) throws ApplicationException;

    void createRepository(String name, URL url) throws ApplicationException;

    void removeRepository(String name) throws ApplicationException;

    void install(String repoName, String appName, List<Integer> version) throws ApplicationException;

    void uninstall(String repoName, String appName, List<Integer> version) throws ApplicationException;

    void migrate(String repoName, String appName, List<Integer> fromVersion, List<Integer> toVersion) throws ApplicationException;

    void start(String repoName, String appName, List<Integer> version) throws ApplicationException;

    void stop(String repoName, String appName, List<Integer> version) throws ApplicationException;

    int getAutoMigrateLevel(final String repoName, final String appName, final List<Integer> version) throws ApplicationException;

    void setAutoMigrateLevel(String repoName, String appName, List<Integer> version, int level) throws ApplicationException;

    void autoMigrate() throws ApplicationException;

}
