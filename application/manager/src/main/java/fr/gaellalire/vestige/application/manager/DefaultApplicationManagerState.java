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

import java.io.Serializable;
import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import fr.gaellalire.vestige.spi.resolver.ResolvedClassLoaderConfiguration;

/**
 * @author Gael Lalire
 */
public class DefaultApplicationManagerState implements Serializable, ApplicationManagerState {

    private static final long serialVersionUID = -6445126871166070449L;

    private Map<String, ApplicationContext> applicationContextByInstallName = new TreeMap<String, ApplicationContext>();

    /**
     * @author Gael Lalire
     */
    private static final class RepositoryContext implements Serializable {

        private static final long serialVersionUID = 2258200173138779520L;

        private URL url;

        private RepositoryContext(final URL url) {
            this.url = url;
        }

    }

    private Map<String, RepositoryContext> urlByRepo = new TreeMap<String, RepositoryContext>();

    // read only

    public Set<Entry<String, ApplicationContext>> applicationContextByInstallNameEntrySet() {
        return applicationContextByInstallName.entrySet();
    }

    public Collection<ApplicationContext> getApplicationContexts() {
        return applicationContextByInstallName.values();
    }

    public boolean hasContext(final String installName) {
        return applicationContextByInstallName.containsKey(installName);
    }

    public URL getRepositoryURL(final String repoName) {
        return urlByRepo.get(repoName).url;
    }

    public ApplicationContext getApplicationContext(final String installName) throws ApplicationException {
        ApplicationContext applicationContext = applicationContextByInstallName.get(installName);
        if (applicationContext == null) {
            throw new ApplicationException("Application " + installName + " is not installed");
        }
        return applicationContext;
    }

    public ApplicationContext getUnlockedApplicationContext(final String installName) throws ApplicationException {
        ApplicationContext applicationContext = getApplicationContext(installName);
        if (applicationContext.isLocked()) {
            throw new ApplicationException("Application " + installName + " is locked");
        }
        return applicationContext;
    }

    public Set<String> getRepositoriesName() {
        return new HashSet<String>(urlByRepo.keySet());
    }

    public Set<String> getApplicationsName() {
        return new HashSet<String>(applicationContextByInstallName.keySet());
    }

    // state change

    public void removeRepository(final String name) throws ApplicationException {
        RepositoryContext remove = urlByRepo.get(name);
        if (remove == null) {
            throw new ApplicationException("Repository do not exists");
        }
        urlByRepo.remove(name);
    }

    public void uninstall(final String installName) throws ApplicationException {
        applicationContextByInstallName.remove(installName);
    }

    public void install(final String installName, final ApplicationContext applicationContext) throws ApplicationException {
        applicationContextByInstallName.put(installName, applicationContext);
    }

    public void createRepository(final String name, final URL url) throws ApplicationException {
        RepositoryContext old = urlByRepo.put(name, new RepositoryContext(url));
        if (old != null) {
            urlByRepo.put(name, old);
            throw new ApplicationException("Repository with name " + name + " already exists");
        }
    }

    @Override
    public URL getApplicationRepositoryURL(final String installName) throws ApplicationException {
        final ApplicationContext applicationContext = getApplicationContext(installName);
        return applicationContext.getRepoURL();
    }

    @Override
    public String getRepositoryApplicationName(final String installName) throws ApplicationException {
        final ApplicationContext applicationContext = getApplicationContext(installName);
        return applicationContext.getRepoApplicationName();
    }

    @Override
    public List<Integer> getRepositoryApplicationVersion(final String installName) throws ApplicationException {
        final ApplicationContext applicationContext = getApplicationContext(installName);
        return applicationContext.getRepoApplicationVersion();
    }

    @Override
    public boolean isAutoStarted(final String installName) throws ApplicationException {
        final ApplicationContext applicationContext = getApplicationContext(installName);
        return applicationContext.isAutoStarted();
    }

    public boolean isStarted(final String installName) throws ApplicationException {
        final ApplicationContext applicationContext = getApplicationContext(installName);
        return applicationContext.isStarted();
    }

    public ResolvedClassLoaderConfiguration getClassLoaders(final String installName) throws ApplicationException {
        final ApplicationContext applicationContext = getApplicationContext(installName);
        return applicationContext.getLauncherAttachmentContext().getResolve();
    }

    public int getAutoMigrateLevel(final String installName) throws ApplicationException {
        ApplicationContext applicationContext = getApplicationContext(installName);
        return applicationContext.getAutoMigrateLevel();
    }

    @Override
    public String getExceptionStackTrace(final String installName) throws ApplicationException {
        final ApplicationContext applicationContext = getApplicationContext(installName);
        return applicationContext.getExceptionStackTrace();
    }

    public ApplicationManagerState copy() {
        DefaultApplicationManagerState defaultApplicationManagerState = new DefaultApplicationManagerState();
        defaultApplicationManagerState.urlByRepo.putAll(urlByRepo);
        for (Entry<String, ApplicationContext> entry : applicationContextByInstallName.entrySet()) {
            defaultApplicationManagerState.applicationContextByInstallName.put(entry.getKey(), entry.getValue().copy());
        }
        return defaultApplicationManagerState;
    }

    @Override
    public List<Integer> getMigrationRepositoryApplicationVersion(final String installName) throws ApplicationException {
        ApplicationContext applicationContext = getApplicationContext(installName);
        ApplicationContext migrationApplicationContext = applicationContext.getMigrationApplicationContext();
        if (migrationApplicationContext != null && !applicationContext.isUncommitted()) {
            return migrationApplicationContext.getRepoApplicationVersion();
        }
        return null;
    }

}
