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

import java.io.File;
import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.security.Permission;
import java.util.List;
import java.util.Set;

import com.googlecode.vestige.platform.ClassLoaderConfiguration;


/**
 * @author Gael Lalire
 */
public class ApplicationContext implements Serializable {

    private static final long serialVersionUID = -63902830158746259L;

    private Set<Permission> permissions;

    private Set<Permission> installerPermissions;

    private ClassLoaderConfiguration resolve;

    private ClassLoaderConfiguration installerResolve;

    private String className;

    private String installerClassName;

    private String name;

    private File home;

    private int autoMigrateLevel;

    private boolean started;

    private boolean installerPrivateSystem;

    private boolean privateSystem;

    private Set<List<Integer>> supportedMigrationVersion;

    private Set<List<Integer>> uninterruptedMigrationVersion;

    private transient WeakReference<RuntimeApplicationContext> runtimeApplicationContext;

    private transient Thread thread;

    public ClassLoaderConfiguration getResolve() {
        return resolve;
    }

    public void setResolve(final ClassLoaderConfiguration resolve) {
        this.resolve = resolve;
    }

    public ClassLoaderConfiguration getInstallerResolve() {
        return installerResolve;
    }

    public void setInstallerResolve(final ClassLoaderConfiguration installerResolve) {
        this.installerResolve = installerResolve;
    }

    public boolean isStarted() {
        return started;
    }

    public void setStarted(final boolean started) {
        this.started = started;
    }

    public File getHome() {
        return home;
    }

    public void setHome(final File home) {
        this.home = home;
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

    public Thread getThread() {
        return thread;
    }

    public void setThread(final Thread thread) {
        this.thread = thread;
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

}
