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

    public ApplicationInstallerInvoker(final Object applicationInstaller) {
        this.applicationInstaller = applicationInstaller;
        Class<? extends Object> applicationInstallerClass = applicationInstaller.getClass();
        for (Method method : ApplicationInstaller.class.getMethods()) {
            String methodName = method.getName();
            try {
                if (methodName.equals("install")) {
                    installMethod = applicationInstallerClass.getMethod(methodName, method.getParameterTypes());
                } else if (methodName.equals("uninstall")) {
                    uninstallMethod = applicationInstallerClass.getMethod(methodName, method.getParameterTypes());
                } else if (methodName.equals("migrateFrom")) {
                    migrateFromMethod = applicationInstallerClass.getMethod(methodName, method.getParameterTypes());
                } else if (methodName.equals("migrateTo")) {
                    migrateToMethod = applicationInstallerClass.getMethod(methodName, method.getParameterTypes());
                } else if (methodName.equals("uninterruptedMigrateFrom")) {
                    uninterruptedMigrateFromMethod = applicationInstallerClass.getMethod(methodName, method.getParameterTypes());
                } else if (methodName.equals("uninterruptedMigrateTo")) {
                    uninterruptedMigrateToMethod = applicationInstallerClass.getMethod(methodName, method.getParameterTypes());
                }
            } catch (NoSuchMethodException e) {
                // ok
            } catch (SecurityException e) {
                // ok
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
    public void migrateFrom(final List<Integer> fromVersion) throws Exception {
        if (migrateFromMethod != null) {
            migrateFromMethod.invoke(applicationInstaller, fromVersion);
        }
    }

    @Override
    public void migrateTo(final List<Integer> toVersion) throws Exception {
        if (migrateToMethod != null) {
            migrateToMethod.invoke(applicationInstaller, toVersion);
        }
    }

    @Override
    public void uninterruptedMigrateFrom(final List<Integer> fromVersion, final Object fromRunnable, final Object runnable, final Runnable unlockThread) throws Exception {
        if (uninterruptedMigrateFromMethod != null) {
            uninterruptedMigrateFromMethod.invoke(applicationInstaller, fromVersion, fromRunnable, runnable, unlockThread);
        }
    }

    @Override
    public void uninterruptedMigrateTo(final Object runnable, final List<Integer> toVersion, final Object toRunnable, final Runnable unlockToThread) throws Exception {
        if (uninterruptedMigrateToMethod != null) {
            uninterruptedMigrateToMethod.invoke(applicationInstaller, runnable, toVersion, toRunnable, unlockToThread);
        }
    }
}
