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
public class SynchronizedApplicationManager implements ApplicationManager {

    private ApplicationManager delegate;

    public SynchronizedApplicationManager(final ApplicationManager delegate) {
        this.delegate = delegate;
    }

    public synchronized Set<String> getRepositoriesName() throws ApplicationException {
        return delegate.getRepositoriesName();
    }

    public synchronized URL getRepositoryURL(final String repoName) throws ApplicationException {
        return delegate.getRepositoryURL(repoName);
    }

    public synchronized Set<String> getApplicationsName(final String repo) throws ApplicationException {
        return delegate.getApplicationsName(repo);
    }

    public synchronized Set<List<Integer>> getVersions(final String repo, final String appName) throws ApplicationException {
        return delegate.getVersions(repo, appName);
    }

    public synchronized boolean isStarted(final String repoName, final String appName, final List<Integer> version) throws ApplicationException {
        return delegate.isStarted(repoName, appName, version);
    }

    public synchronized void createRepository(final String name, final URL url) throws ApplicationException {
        delegate.createRepository(name, url);
    }

    public synchronized void removeRepository(final String name) throws ApplicationException {
        delegate.removeRepository(name);
    }

    public synchronized void install(final String repoName, final String appName, final List<Integer> version) throws ApplicationException {
        delegate.install(repoName, appName, version);
    }

    public synchronized void uninstall(final String repoName, final String appName, final List<Integer> version) throws ApplicationException {
        delegate.uninstall(repoName, appName, version);
    }

    public synchronized void migrate(final String repoName, final String appName, final List<Integer> fromVersion, final List<Integer> toVersion)
            throws ApplicationException {
        delegate.migrate(repoName, appName, fromVersion, toVersion);
    }

    public synchronized void start(final String repoName, final String appName, final List<Integer> version) throws ApplicationException {
        delegate.start(repoName, appName, version);
    }

    public synchronized void stop(final String repoName, final String appName, final List<Integer> version) throws ApplicationException {
        delegate.stop(repoName, appName, version);
    }

    public synchronized int getAutoMigrateLevel(final String repoName, final String appName, final List<Integer> version) throws ApplicationException {
        return delegate.getAutoMigrateLevel(repoName, appName, version);
    }

    public synchronized void setAutoMigrateLevel(final String repoName, final String appName, final List<Integer> version, final int level)
            throws ApplicationException {
        delegate.setAutoMigrateLevel(repoName, appName, version, level);
    }

    public synchronized void autoMigrate() throws ApplicationException {
        delegate.autoMigrate();
    }

    @Override
    public ClassLoaderConfiguration getClassLoaders(final String repoName, final String appName, final List<Integer> version)
            throws ApplicationException {
        return delegate.getClassLoaders(repoName, appName, version);
    }

}
