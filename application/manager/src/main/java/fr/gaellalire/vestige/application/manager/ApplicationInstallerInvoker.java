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

import java.lang.reflect.Method;
import java.util.List;

/**
 * @author Gael Lalire
 */
public class ApplicationInstallerInvoker implements ApplicationInstaller {

    private Object applicationInstaller;

    private Method installMethod;

    private Method uninstallMethod;

    private Method migrateFromMethod;

    private Method migrateToMethod;

    private Method uninterruptedMigrateFromMethod;

    private Method uninterruptedMigrateToMethod;

    private Method migrateFromWithInstallerMethod;

    private Method migrateToWithInstallerMethod;

    private Method uninterruptedMigrateFromWithInstallerMethod;

    private Method uninterruptedMigrateToWithInstallerMethod;

    private Method cleanMethod;

    private Method commitMethod;

    public ApplicationInstallerInvoker(final Object applicationInstaller) {
        this.applicationInstaller = applicationInstaller;
        Class<? extends Object> applicationInstallerClass = applicationInstaller.getClass();
        for (Method method : ApplicationInstaller.class.getMethods()) {
            String methodName = method.getName();
            Method applicationInstallerMethod = null;
            try {
                applicationInstallerMethod = applicationInstallerClass.getMethod(methodName, method.getParameterTypes());
            } catch (NoSuchMethodException e) {
                // ok
            } catch (SecurityException e) {
                // ok
            }
            if (methodName.equals("install")) {
                installMethod = applicationInstallerMethod;
            } else if (methodName.equals("uninstall")) {
                uninstallMethod = applicationInstallerMethod;
            } else if (methodName.equals("prepareMigrateFrom")) {
                if (method.getParameterTypes().length == 1) {
                    migrateFromMethod = applicationInstallerMethod;
                } else {
                    migrateFromWithInstallerMethod = applicationInstallerMethod;
                }
            } else if (methodName.equals("prepareMigrateTo")) {
                if (method.getParameterTypes().length == 1) {
                    migrateToMethod = applicationInstallerMethod;
                } else {
                    migrateToWithInstallerMethod = applicationInstallerMethod;
                }
            } else if (methodName.equals("prepareUninterruptedMigrateFrom")) {
                if (method.getParameterTypes().length == 4) {
                    uninterruptedMigrateFromMethod = applicationInstallerMethod;
                } else {
                    uninterruptedMigrateFromWithInstallerMethod = applicationInstallerMethod;
                }
            } else if (methodName.equals("prepareUninterruptedMigrateTo")) {
                if (method.getParameterTypes().length == 4) {
                    uninterruptedMigrateToMethod = applicationInstallerMethod;
                } else {
                    uninterruptedMigrateToWithInstallerMethod = applicationInstallerMethod;
                }
            } else if (methodName.equals("cleanMigration")) {
                cleanMethod = applicationInstallerMethod;
            } else if (methodName.equals("commitMigration")) {
                commitMethod = applicationInstallerMethod;
            }
        }
    }

    @Override
    public void install() throws Exception {
        if (installMethod != null) {
            installMethod.invoke(applicationInstaller);
        }
    }

    @Override
    public void uninstall() throws Exception {
        if (uninstallMethod != null) {
            uninstallMethod.invoke(applicationInstaller);
        }
    }

    @Override
    public void prepareMigrateFrom(final List<Integer> fromVersion) throws Exception {
        if (migrateFromMethod == null) {
            throw new ApplicationException("Method prepareMigrateFrom not found");
        }
        migrateFromMethod.invoke(applicationInstaller, fromVersion);
    }

    @Override
    public void prepareMigrateTo(final List<Integer> toVersion) throws Exception {
        if (migrateToMethod == null) {
            throw new ApplicationException("Method prepareMigrateTo not found");
        }
        migrateToMethod.invoke(applicationInstaller, toVersion);
    }

    @Override
    public void prepareUninterruptedMigrateFrom(final List<Integer> fromVersion, final Object fromRunnable, final Object runnable, final Runnable unlockThread) throws Exception {
        if (uninterruptedMigrateFromMethod == null) {
            throw new ApplicationException("Method prepareUninterruptedMigrateFrom not found");
        }
        uninterruptedMigrateFromMethod.invoke(applicationInstaller, fromVersion, fromRunnable, runnable, unlockThread);
    }

    @Override
    public void prepareUninterruptedMigrateTo(final Object runnable, final List<Integer> toVersion, final Object toRunnable, final Runnable unlockToThread) throws Exception {
        if (uninterruptedMigrateToMethod == null) {
            throw new ApplicationException("Method prepareUninterruptedMigrateTo not found");
        }
        uninterruptedMigrateToMethod.invoke(applicationInstaller, runnable, toVersion, toRunnable, unlockToThread);
    }

    @Override
    public void cleanMigration() throws Exception {
        if (cleanMethod == null) {
            throw new ApplicationException("Method cleanMigration not found");
        }
        cleanMethod.invoke(applicationInstaller);
    }

    @Override
    public void commitMigration() throws Exception {
        if (commitMethod == null) {
            throw new ApplicationException("Method commitMigration not found");
        }
        commitMethod.invoke(applicationInstaller);
    }

    public boolean hasMigrateFromWithInstaller() {
        return migrateFromWithInstallerMethod != null;
    }

    @Override
    public void prepareMigrateFrom(final List<Integer> fromVersion, final Object fromVersionInstaller) throws Exception {
        if (migrateFromWithInstallerMethod == null) {
            throw new ApplicationException("Method prepareMigrateFrom with installer not found");
        }
        migrateFromWithInstallerMethod.invoke(applicationInstaller, fromVersion, fromVersionInstaller);
    }

    public boolean hasMigrateToWithInstaller() {
        return migrateToWithInstallerMethod != null;
    }

    @Override
    public void prepareMigrateTo(final List<Integer> toVersion, final Object toVersionInstaller) throws Exception {
        if (migrateToWithInstallerMethod == null) {
            throw new ApplicationException("Method prepareMigrateTo with installer not found");
        }
        migrateToWithInstallerMethod.invoke(applicationInstaller, toVersion, toVersionInstaller);
    }

    public boolean hasUninterruptedMigrateFromWithInstaller() {
        return uninterruptedMigrateFromWithInstallerMethod != null;
    }

    @Override
    public void prepareUninterruptedMigrateFrom(final List<Integer> fromVersion, final Object fromRunnable, final Object runnable, final Runnable unlockThread,
            final Object fromVersionInstaller) throws Exception {
        if (uninterruptedMigrateFromWithInstallerMethod == null) {
            throw new ApplicationException("Method prepareUninterruptedMigrateFrom with installer not found");
        }
        uninterruptedMigrateFromWithInstallerMethod.invoke(applicationInstaller, fromVersion, fromRunnable, runnable, unlockThread, fromVersionInstaller);
    }

    public boolean hasUninterruptedMigrateToWithInstaller() {
        return uninterruptedMigrateToWithInstallerMethod != null;
    }

    @Override
    public void prepareUninterruptedMigrateTo(final Object runnable, final List<Integer> toVersion, final Object toRunnable, final Runnable unlockToThread,
            final Object toVersionInstaller) throws Exception {
        if (uninterruptedMigrateToWithInstallerMethod == null) {
            throw new ApplicationException("Method prepareUninterruptedMigrateTo with installer not found");
        }
        uninterruptedMigrateToWithInstallerMethod.invoke(applicationInstaller, runnable, toVersion, toRunnable, unlockToThread, toVersionInstaller);
    }
}
