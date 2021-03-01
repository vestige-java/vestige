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
import java.io.FilePermission;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessController;
import java.security.Permission;
import java.security.Permissions;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import fr.gaellalire.vestige.spi.system.VestigeSystem;
import fr.gaellalire.vestige.spi.system.VestigeSystemCache;
import fr.gaellalire.vestige.system.PrivateVestigePolicy;
import fr.gaellalire.vestige.system.PrivateVestigeSecurityManager;

/**
 * @author Gael Lalire
 */
public class VestigeSecureExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(VestigeSecureExecutor.class);

    public static final String VESTIGE_APP_NAME = "vestige.appName";

    private PrivateVestigeSecurityManager vestigeSecurityManager;

    private PrivateVestigePolicy vestigePolicy;

    private Set<Permission> systemResourcePermissions;

    private ThreadGroupDestroyer threadGroupDestroyer;

    private VestigeSystem handlerVestigeSystem;

    public VestigeSecureExecutor(final PrivateVestigeSecurityManager vestigeSecurityManager, final PrivateVestigePolicy vestigePolicy, final VestigeSystem handlerVestigeSystem) {
        this.vestigeSecurityManager = vestigeSecurityManager;
        this.vestigePolicy = vestigePolicy;
        this.handlerVestigeSystem = handlerVestigeSystem;
        Set<Permission> resourcesPermissions = new HashSet<Permission>();
        try {
            resourcesPermissions.add(ClassLoader.getSystemResource("java/lang/Object.class").openConnection().getPermission());
            for (URL url : ((URLClassLoader) ClassLoader.getSystemClassLoader()).getURLs()) {
                resourcesPermissions.add(url.openConnection().getPermission());
            }
        } catch (Exception e) {
            LOGGER.debug("Unable to add permission to system classloader", e);
            String classPath = System.getProperty("java.class.path");
            if (classPath != null) {
                StringTokenizer extensionsTok = new StringTokenizer(classPath, File.pathSeparator);
                while (extensionsTok.hasMoreTokens()) {
                    String path = new File(extensionsTok.nextToken()).getPath();
                    resourcesPermissions.add(new FilePermission(path, "read"));
                    resourcesPermissions.add(new FilePermission(path + File.separator + "*", "read"));
                }
            }
        }
        String extdir = System.getProperty("java.ext.dirs");
        if (extdir != null) {
            StringTokenizer extensionsTok = new StringTokenizer(extdir, File.pathSeparator);
            while (extensionsTok.hasMoreTokens()) {
                String path = new File(extensionsTok.nextToken()).getPath();
                resourcesPermissions.add(new FilePermission(path, "read"));
                resourcesPermissions.add(new FilePermission(path + File.separator + "-", "read"));
            }
        }
        String libdir = System.getProperty("java.library.path");
        if (libdir != null) {
            StringTokenizer extensionsTok = new StringTokenizer(libdir, File.pathSeparator);
            while (extensionsTok.hasMoreTokens()) {
                String path = new File(extensionsTok.nextToken()).getPath();
                resourcesPermissions.add(new FilePermission(path, "read"));
                resourcesPermissions.add(new FilePermission(path + File.separator + "*", "read"));
            }
        }
        systemResourcePermissions = Collections.unmodifiableSet(resourcesPermissions);
    }

    public <E> VestigeSecureExecution<E> execute(final ClassLoader contextClassLoader, final Set<Permission> additionnalPermissions, final List<ThreadGroup> threadGroups,
            final String name, final VestigeSystem appVestigeSystem, final VestigeSecureCallable<E> callable, final FutureDoneHandler<E> doneHandler) {
        return execute(contextClassLoader, additionnalPermissions, threadGroups, name, appVestigeSystem, callable, doneHandler, false, false);
    }

    public <E> VestigeSecureExecution<E> execute(final ClassLoader contextClassLoader, final Set<Permission> additionnalPermissions, final List<ThreadGroup> threadGroups,
            final String name, final VestigeSystem appVestigeSystem, final VestigeSecureCallable<E> callable, final FutureDoneHandler<E> doneHandler, final boolean selfThread,
            final boolean interrupted) {
        final ThreadGroup threadGroup;
        if (selfThread) {
            threadGroup = null;
        } else {
            threadGroup = new ThreadGroup(name);
        }
        final List<ThreadGroup> accessibleThreadGroups;
        final Permissions permissions;

        if (vestigeSecurityManager != null) {
            if (threadGroups != null) {
                accessibleThreadGroups = new ArrayList<ThreadGroup>(threadGroups.size() + 1);
                accessibleThreadGroups.addAll(threadGroups);
                if (!selfThread) {
                    accessibleThreadGroups.add(threadGroup);
                }
            } else {
                if (selfThread) {
                    accessibleThreadGroups = Collections.emptyList();
                } else {
                    accessibleThreadGroups = Collections.singletonList(threadGroup);
                }
            }
            permissions = new Permissions();
            // getResource for system jar
            for (Permission permission : systemResourcePermissions) {
                permissions.add(permission);
            }
            // access to tmp dir
            String tmpdir = System.getProperty("java.io.tmpdir");
            if (tmpdir != null) {
                String path = new File(tmpdir).getPath();
                permissions.add(new FilePermission(path, "read,write"));
                permissions.add(new FilePermission(path + File.separator + "-", "read,write,delete"));
            }
            // additionnalPermissions
            for (Permission permission : additionnalPermissions) {
                permissions.add(permission);
            }
        } else {
            accessibleThreadGroups = null;
            permissions = null;
        }

        final PrivilegedExceptionActionExecutor privilegedExecutor;
        if (appVestigeSystem != null && vestigeSecurityManager != null) {
            privilegedExecutor = new PrivilegedExceptionActionExecutor() {

                @Override
                public void setPrivileged() {
                    vestigePolicy.unsetPermissionCollection();
                    vestigeSecurityManager.unsetThreadGroups();
                    handlerVestigeSystem.setCurrentSystem();
                }

                @Override
                public void unsetPrivileged() {
                    vestigeSecurityManager.setThreadGroups(accessibleThreadGroups);
                    vestigePolicy.setPermissionCollection(permissions);
                    appVestigeSystem.setCurrentSystem();
                }

            };
        } else {
            privilegedExecutor = PrivilegedExceptionActionExecutor.DIRECT_EXECUTOR;
        }

        FutureTask<E> futureTask = new FutureTask<E>(new Callable<E>() {
            @Override
            public E call() throws Exception {
                MDC.put(VESTIGE_APP_NAME, name);
                try {
                    if (vestigeSecurityManager != null) {
                        if (selfThread) {
                            List<ThreadGroup> localAccessibleThreadGroups = new ArrayList<ThreadGroup>(accessibleThreadGroups.size() + 1);
                            localAccessibleThreadGroups.add(Thread.currentThread().getThreadGroup());
                            vestigeSecurityManager.setThreadGroups(localAccessibleThreadGroups);
                        } else {
                            vestigeSecurityManager.setThreadGroups(accessibleThreadGroups);
                        }
                        vestigePolicy.setPermissionCollection(permissions);
                        try {
                            return AccessController.doPrivileged(new PrivilegedExceptionAction<E>() {
                                @Override
                                public E run() throws Exception {
                                    appVestigeSystem.setCurrentSystem();
                                    VestigeSystemCache vestigeSystemCache = appVestigeSystem.pushVestigeSystemCache();
                                    try {
                                        if (interrupted) {
                                            Thread.currentThread().interrupt();
                                        }
                                        return callable.call(privilegedExecutor);
                                    } finally {
                                        vestigeSystemCache.clearCache();
                                    }
                                }
                            });
                        } catch (PrivilegedActionException e) {
                            throw e.getException();
                        }
                    } else {
                        appVestigeSystem.setCurrentSystem();
                        VestigeSystemCache vestigeSystemCache = appVestigeSystem.pushVestigeSystemCache();
                        try {
                            if (interrupted) {
                                Thread.currentThread().interrupt();
                            }
                            return callable.call(privilegedExecutor);
                        } finally {
                            vestigeSystemCache.clearCache();
                        }
                    }
                } finally {
                    if (vestigeSecurityManager != null) {
                        vestigePolicy.unsetPermissionCollection();
                        vestigeSecurityManager.unsetThreadGroups();
                    }
                    handlerVestigeSystem.setCurrentSystem();
                }
            }
        }) {
            @Override
            protected void done() {
                try {
                    if (doneHandler != null) {
                        doneHandler.futureDone(this);
                    }
                } finally {
                    if (threadGroupDestroyer != null) {
                        threadGroupDestroyer.destroy(Thread.currentThread(), threadGroup);
                    }
                    MDC.remove(VESTIGE_APP_NAME);
                }
            }
        };
        if (selfThread) {
            return new SelfThreadVestigeSecureExecution<E>(futureTask);
        } else {
            Thread thread = new Thread(threadGroup, futureTask, "main");
            thread.setContextClassLoader(contextClassLoader);
            return new NewThreadVestigeSecureExecution<E>(thread, futureTask);
        }
    }

    public void startService() {
        threadGroupDestroyer = new ThreadGroupDestroyer();
        threadGroupDestroyer.start();
    }

    public void stopService() {
        threadGroupDestroyer.interrupt();
        try {
            threadGroupDestroyer.join();
        } catch (InterruptedException e) {
            LOGGER.warn("Interrupted while stopping", e);
        } finally {
            threadGroupDestroyer = null;
        }
    }

}
