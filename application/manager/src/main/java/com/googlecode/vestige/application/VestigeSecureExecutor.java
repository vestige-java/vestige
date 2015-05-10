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
import java.io.FilePermission;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessController;
import java.security.Permission;
import java.security.Permissions;
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

import com.googlecode.vestige.platform.system.PrivateVestigePolicy;
import com.googlecode.vestige.platform.system.PrivateVestigeSecurityManager;
import com.googlecode.vestige.platform.system.PublicVestigeSystem;

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

    public VestigeSecureExecutor(final PrivateVestigeSecurityManager vestigeSecurityManager, final PrivateVestigePolicy vestigePolicy) {
        this.vestigeSecurityManager = vestigeSecurityManager;
        this.vestigePolicy = vestigePolicy;
        Set<Permission> resourcesPermissions = new HashSet<Permission>();
        try {
            resourcesPermissions.add(ClassLoader.getSystemResource("java/lang/Object.class").openConnection().getPermission());
            for (URL url : ((URLClassLoader) ClassLoader.getSystemClassLoader()).getURLs()) {
                resourcesPermissions.add(url.openConnection().getPermission());
            }
        } catch (Exception e) {
            LOGGER.debug("Unable to add permission to system classloader", e);
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
        threadGroupDestroyer = new ThreadGroupDestroyer();
        threadGroupDestroyer.start();
    }

    public <E> Thread execute(final Set<Permission> additionnalPermissions, final List<ThreadGroup> threadGroups,  final String name, final PublicVestigeSystem appVestigeSystem, final Callable<E> callable,
            final PublicVestigeSystem handlerVestigeSystem, final FutureDoneHandler<E> done) {
        final ThreadGroup threadGroup = new ThreadGroup(name);
        FutureTask<E> futureTask = new FutureTask<E>(new Callable<E>() {
            @Override
            public E call() throws Exception {
                MDC.put(VESTIGE_APP_NAME, name);
                try {
                    if (appVestigeSystem != null) {
                        appVestigeSystem.setCurrentSystem();
                    }
                    if (vestigeSecurityManager != null) {
                        if (threadGroups != null) {
                            List<ThreadGroup> accessibleThreadGroups = new ArrayList<ThreadGroup>(threadGroups.size() + 1);
                            accessibleThreadGroups.addAll(threadGroups);
                            accessibleThreadGroups.add(threadGroup);
                            vestigeSecurityManager.setThreadGroups(accessibleThreadGroups);
                        } else {
                            vestigeSecurityManager.setThreadGroups(Collections.singletonList(threadGroup));
                        }
                        Permissions permissions = new Permissions();
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
                        vestigePolicy.setPermissionCollection(permissions);
                    }
                    if (vestigeSecurityManager != null) {
                        return AccessController.doPrivileged(new PrivilegedExceptionAction<E>() {
                            @Override
                            public E run() throws Exception {
                                return callable.call();
                            }
                        });
                    } else {
                        return callable.call();
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
                if (done != null) {
                    done.futureDone(this);
                }
                threadGroupDestroyer.destroy(Thread.currentThread(), threadGroup);
                MDC.remove(VESTIGE_APP_NAME);
            }
        };
        return new Thread(threadGroup, futureTask, "main");
    }

    public void stop() {
        threadGroupDestroyer.interrupt();
        try {
            threadGroupDestroyer.join();
        } catch (InterruptedException e) {
            LOGGER.warn("Interrupted while stopping", e);
        }
    }
}
