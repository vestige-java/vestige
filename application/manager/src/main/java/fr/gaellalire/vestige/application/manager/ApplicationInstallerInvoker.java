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
                migrateFromMethod = applicationInstallerMethod;
            } else if (methodName.equals("prepareMigrateTo")) {
                migrateToMethod = applicationInstallerMethod;
            } else if (methodName.equals("prepareUninterruptedMigrateFrom")) {
                uninterruptedMigrateFromMethod = applicationInstallerMethod;
            } else if (methodName.equals("prepareUninterruptedMigrateTo")) {
                uninterruptedMigrateToMethod = applicationInstallerMethod;
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
}
