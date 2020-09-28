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

import java.io.File;
import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.security.Permission;
import java.util.List;
import java.util.Set;

/**
 * @author Gael Lalire
 */
public class ApplicationContext implements Serializable, Cloneable {

    private static final long serialVersionUID = -63902830158746259L;

    private Set<Permission> permissions;

    private Set<Permission> installerPermissions;

    private transient ApplicationResolvedClassLoaderConfiguration resolve;

    private transient ApplicationResolvedClassLoaderConfiguration installerResolve;

    private String className;

    private String installerClassName;

    private String name;

    private File config;

    private File data;

    private File cache;

    private int autoMigrateLevel;

    private boolean installerPrivateSystem;

    private boolean privateSystem;

    private Set<List<Integer>> supportedMigrationVersion;

    private Set<List<Integer>> uninterruptedMigrationVersion;

    private URL repoURL;

    private boolean autoStarted;

    private boolean uncommitted;

    private transient boolean locked;

    private List<AddInject> addInjects;

    private List<AddInject> installerAddInjects;

    private URL overrideURL;

    public List<AddInject> getInstallerAddInjects() {
        return installerAddInjects;
    }

    public void setInstallerAddInjects(final List<AddInject> installerAddInjects) {
        this.installerAddInjects = installerAddInjects;
    }

    public List<AddInject> getAddInjects() {
        return addInjects;
    }

    public void setAddInjects(final List<AddInject> addInjects) {
        this.addInjects = addInjects;
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(final boolean locked) {
        this.locked = locked;
    }

    public boolean isUncommitted() {
        return uncommitted;
    }

    public void setUncommitted(final boolean uncommitted) {
        this.uncommitted = uncommitted;
    }

    public boolean isAutoStarted() {
        return autoStarted;
    }

    public void setAutoStarted(final boolean autoStarted) {
        this.autoStarted = autoStarted;
    }

    public URL getRepoURL() {
        return repoURL;
    }

    public void setRepoURL(final URL repoURL) {
        this.repoURL = repoURL;
    }

    public String getRepoApplicationName() {
        return repoApplicationName;
    }

    public void setRepoApplicationName(final String repoApplicationName) {
        this.repoApplicationName = repoApplicationName;
    }

    private ApplicationContext migrationApplicationContext;

    public void setMigrationApplicationContext(final ApplicationContext migrationApplicationContext) {
        this.migrationApplicationContext = migrationApplicationContext;
    }

    public ApplicationContext getMigrationApplicationContext() {
        return migrationApplicationContext;
    }

    public List<Integer> getRepoApplicationVersion() {
        return repoApplicationVersion;
    }

    public void setRepoApplicationVersion(final List<Integer> repoApplicationVersion) {
        this.repoApplicationVersion = repoApplicationVersion;
    }

    private String repoApplicationName;

    private List<Integer> repoApplicationVersion;

    private transient WeakReference<RuntimeApplicationInstallerContext> runtimeApplicationInstallerContext;

    private transient WeakReference<RuntimeApplicationContext> runtimeApplicationContext;

    private transient VestigeSecureExecution<Void> vestigeSecureExecution;

    private transient boolean started;

    private transient Exception exception;

    public ApplicationResolvedClassLoaderConfiguration getResolve() {
        return resolve;
    }

    public void setResolve(final ApplicationResolvedClassLoaderConfiguration resolve) {
        this.resolve = resolve;
    }

    public ApplicationResolvedClassLoaderConfiguration getInstallerResolve() {
        return installerResolve;
    }

    public void setInstallerResolve(final ApplicationResolvedClassLoaderConfiguration installerResolve) {
        this.installerResolve = installerResolve;
    }

    public boolean isStarted() {
        return started;
    }

    public void setStarted(final boolean started) {
        this.started = started;
    }

    public Exception getException() {
        return exception;
    }

    public void setException(final Exception exception) {
        this.exception = exception;
    }

    public File getConfig() {
        return config;
    }

    public void setConfig(final File config) {
        this.config = config;
    }

    public RuntimeApplicationInstallerContext getRuntimeApplicationInstallerContext() {
        if (runtimeApplicationInstallerContext == null) {
            return null;
        }
        return runtimeApplicationInstallerContext.get();
    }

    public void setRuntimeApplicationInstallerContext(final RuntimeApplicationInstallerContext runtimeApplicationInstallerContext) {
        this.runtimeApplicationInstallerContext = new WeakReference<RuntimeApplicationInstallerContext>(runtimeApplicationInstallerContext);
    }

    public RuntimeApplicationContext getRuntimeApplicationContext() {
        if (runtimeApplicationContext == null) {
            return null;
        }
        return runtimeApplicationContext.get();
    }

    public void setRuntimeApplicationContext(final RuntimeApplicationContext runtimeApplicationContext) {
        this.runtimeApplicationContext = new WeakReference<RuntimeApplicationContext>(runtimeApplicationContext);
    }

    public VestigeSecureExecution<Void> getVestigeSecureExecution() {
        return vestigeSecureExecution;
    }

    public void setVestigeSecureExecution(final VestigeSecureExecution<Void> vestigeSecureExecution) {
        this.vestigeSecureExecution = vestigeSecureExecution;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(final String className) {
        this.className = className;
    }

    public String getInstallerClassName() {
        return installerClassName;
    }

    public void setInstallerClassName(final String installerClassName) {
        this.installerClassName = installerClassName;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public int getAutoMigrateLevel() {
        return autoMigrateLevel;
    }

    public void setAutoMigrateLevel(final int autoMigrateLevel) {
        this.autoMigrateLevel = autoMigrateLevel;
    }

    public Set<List<Integer>> getSupportedMigrationVersion() {
        return supportedMigrationVersion;
    }

    public void setSupportedMigrationVersion(final Set<List<Integer>> supportedMigrationVersion) {
        this.supportedMigrationVersion = supportedMigrationVersion;
    }

    public Set<List<Integer>> getUninterruptedMigrationVersion() {
        return uninterruptedMigrationVersion;
    }

    public void setUninterruptedMigrationVersion(final Set<List<Integer>> uninterruptedMigrationVersion) {
        this.uninterruptedMigrationVersion = uninterruptedMigrationVersion;
    }

    public boolean isInstallerPrivateSystem() {
        return installerPrivateSystem;
    }

    public void setInstallerPrivateSystem(final boolean installerPrivateSystem) {
        this.installerPrivateSystem = installerPrivateSystem;
    }

    public boolean isPrivateSystem() {
        return privateSystem;
    }

    public void setPrivateSystem(final boolean privateSystem) {
        this.privateSystem = privateSystem;
    }

    public Set<Permission> getPermissions() {
        return permissions;
    }

    public void setPermissions(final Set<Permission> permissions) {
        this.permissions = permissions;
    }

    public Set<Permission> getInstallerPermissions() {
        return installerPermissions;
    }

    public void setInstallerPermissions(final Set<Permission> installerPermissions) {
        this.installerPermissions = installerPermissions;
    }

    public File getData() {
        return data;
    }

    public void setData(final File data) {
        this.data = data;
    }

    public File getCache() {
        return cache;
    }

    public void setCache(final File cache) {
        this.cache = cache;
    }

    public URL getOverrideURL() {
        return overrideURL;
    }

    public void setOverrideURL(final URL overrideURL) {
        this.overrideURL = overrideURL;
    }

    // private void writeObject(ObjectOutputStream out) throws IOException {
    // out.defaultWriteObject();
    // resolve.save(out);
    // installerResolve.save(out);
    // }
    //
    // private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
    // in.defaultReadObject();
    // resolve = in.readObject();
    //
    // }

    public ApplicationContext copy() {
        try {
            return (ApplicationContext) clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

}
