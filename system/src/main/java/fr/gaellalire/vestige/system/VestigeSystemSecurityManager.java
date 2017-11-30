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

package fr.gaellalire.vestige.system;

import java.io.FileDescriptor;
import java.net.InetAddress;
import java.security.Permission;

/**
 * @author Gael Lalire
 */
@SuppressWarnings("deprecation")
public class VestigeSystemSecurityManager extends SecurityManager {

    private VestigeSystemHolder vestigeSystemHolder;

    private DefaultVestigeSystem vestigeSystem;

    private VestigeSystemSecurityManager previousSecurityManager;

    private SecurityManager securityManager;

    public VestigeSystemSecurityManager(final VestigeSystemHolder vestigeSystemHolder, final DefaultVestigeSystem vestigeSystem, final VestigeSystemSecurityManager previousSecurityManager) {
        this.vestigeSystemHolder = vestigeSystemHolder;
        this.vestigeSystem = vestigeSystem;
        this.previousSecurityManager = previousSecurityManager;
    }

    public SecurityManager getSecurityManager() {
        return securityManager;
    }

    public void setSecurityManager(final SecurityManager securityManager) {
        this.securityManager = securityManager;
    }

    @Override
    public void checkAccept(final String host, final int port) {
        DefaultVestigeSystem currentVestigeSystem = vestigeSystemHolder.getVestigeSystem();
        vestigeSystemHolder.setVestigeSystem(vestigeSystem);
        try {
            if (previousSecurityManager != null) {
                previousSecurityManager.checkAccept(host, port);
            }
            SecurityManager securityManager = getSecurityManager();
            if (securityManager != null) {
                securityManager.checkAccept(host, port);
            }
        } finally {
            vestigeSystemHolder.setVestigeSystem(currentVestigeSystem);
        }
    }

    @Override
    public void checkAccess(final Thread t) {
        DefaultVestigeSystem currentVestigeSystem = vestigeSystemHolder.getVestigeSystem();
        vestigeSystemHolder.setVestigeSystem(vestigeSystem);
        try {
            if (previousSecurityManager != null) {
                previousSecurityManager.checkAccess(t);
            }
            SecurityManager securityManager = getSecurityManager();
            if (securityManager != null) {
                securityManager.checkAccess(t);
            }
        } finally {
            vestigeSystemHolder.setVestigeSystem(currentVestigeSystem);
        }
    }

    @Override
    public void checkAccess(final ThreadGroup g) {
        DefaultVestigeSystem currentVestigeSystem = vestigeSystemHolder.getVestigeSystem();
        vestigeSystemHolder.setVestigeSystem(vestigeSystem);
        try {
            if (previousSecurityManager != null) {
                previousSecurityManager.checkAccess(g);
            }
            SecurityManager securityManager = getSecurityManager();
            if (securityManager != null) {
                securityManager.checkAccess(g);
            }
        } finally {
            vestigeSystemHolder.setVestigeSystem(currentVestigeSystem);
        }
    }

    @Override
    public void checkAwtEventQueueAccess() {
        DefaultVestigeSystem currentVestigeSystem = vestigeSystemHolder.getVestigeSystem();
        vestigeSystemHolder.setVestigeSystem(vestigeSystem);
        try {
            if (previousSecurityManager != null) {
                previousSecurityManager.checkAwtEventQueueAccess();
            }
            SecurityManager securityManager = getSecurityManager();
            if (securityManager != null) {
                securityManager.checkAwtEventQueueAccess();
            }
        } finally {
            vestigeSystemHolder.setVestigeSystem(currentVestigeSystem);
        }
    }

    @Override
    public void checkConnect(final String host, final int port) {
        DefaultVestigeSystem currentVestigeSystem = vestigeSystemHolder.getVestigeSystem();
        vestigeSystemHolder.setVestigeSystem(vestigeSystem);
        try {
            if (previousSecurityManager != null) {
                previousSecurityManager.checkConnect(host, port);
            }
            SecurityManager securityManager = getSecurityManager();
            if (securityManager != null) {
                securityManager.checkConnect(host, port);
            }
        } finally {
            vestigeSystemHolder.setVestigeSystem(currentVestigeSystem);
        }
    }

    @Override
    public void checkConnect(final String host, final int port, final Object context) {
        DefaultVestigeSystem currentVestigeSystem = vestigeSystemHolder.getVestigeSystem();
        vestigeSystemHolder.setVestigeSystem(vestigeSystem);
        try {
            if (previousSecurityManager != null) {
                previousSecurityManager.checkConnect(host, port, context);
            }
            SecurityManager securityManager = getSecurityManager();
            if (securityManager != null) {
                securityManager.checkConnect(host, port, context);
            }
        } finally {
            vestigeSystemHolder.setVestigeSystem(currentVestigeSystem);
        }
    }

    @Override
    public void checkCreateClassLoader() {
        DefaultVestigeSystem currentVestigeSystem = vestigeSystemHolder.getVestigeSystem();
        vestigeSystemHolder.setVestigeSystem(vestigeSystem);
        try {
            if (previousSecurityManager != null) {
                previousSecurityManager.checkCreateClassLoader();
            }
            SecurityManager securityManager = getSecurityManager();
            if (securityManager != null) {
                securityManager.checkCreateClassLoader();
            }
        } finally {
            vestigeSystemHolder.setVestigeSystem(currentVestigeSystem);
        }
    }

    @Override
    public void checkDelete(final String file) {
        DefaultVestigeSystem currentVestigeSystem = vestigeSystemHolder.getVestigeSystem();
        vestigeSystemHolder.setVestigeSystem(vestigeSystem);
        try {
            if (previousSecurityManager != null) {
                previousSecurityManager.checkDelete(file);
            }
            SecurityManager securityManager = getSecurityManager();
            if (securityManager != null) {
                securityManager.checkDelete(file);
            }
        } finally {
            vestigeSystemHolder.setVestigeSystem(currentVestigeSystem);
        }
    }

    @Override
    public void checkExec(final String cmd) {
        DefaultVestigeSystem currentVestigeSystem = vestigeSystemHolder.getVestigeSystem();
        vestigeSystemHolder.setVestigeSystem(vestigeSystem);
        try {
            if (previousSecurityManager != null) {
                previousSecurityManager.checkExec(cmd);
            }
            SecurityManager securityManager = getSecurityManager();
            if (securityManager != null) {
                securityManager.checkExec(cmd);
            }
        } finally {
            vestigeSystemHolder.setVestigeSystem(currentVestigeSystem);
        }
    }

    @Override
    public void checkExit(final int status) {
        DefaultVestigeSystem currentVestigeSystem = vestigeSystemHolder.getVestigeSystem();
        vestigeSystemHolder.setVestigeSystem(vestigeSystem);
        try {
            if (previousSecurityManager != null) {
                previousSecurityManager.checkExit(status);
            }
            SecurityManager securityManager = getSecurityManager();
            if (securityManager != null) {
                securityManager.checkExit(status);
            }
        } finally {
            vestigeSystemHolder.setVestigeSystem(currentVestigeSystem);
        }
    }

    @Override
    public void checkLink(final String lib) {
        DefaultVestigeSystem currentVestigeSystem = vestigeSystemHolder.getVestigeSystem();
        vestigeSystemHolder.setVestigeSystem(vestigeSystem);
        try {
            if (previousSecurityManager != null) {
                previousSecurityManager.checkLink(lib);
            }
            SecurityManager securityManager = getSecurityManager();
            if (securityManager != null) {
                securityManager.checkLink(lib);
            }
        } finally {
            vestigeSystemHolder.setVestigeSystem(currentVestigeSystem);
        }
    }

    @Override
    public void checkListen(final int port) {
        DefaultVestigeSystem currentVestigeSystem = vestigeSystemHolder.getVestigeSystem();
        vestigeSystemHolder.setVestigeSystem(vestigeSystem);
        try {
            if (previousSecurityManager != null) {
                previousSecurityManager.checkListen(port);
            }
            SecurityManager securityManager = getSecurityManager();
            if (securityManager != null) {
                securityManager.checkListen(port);
            }
        } finally {
            vestigeSystemHolder.setVestigeSystem(currentVestigeSystem);
        }
    }

    @Override
    public void checkMemberAccess(final Class<?> clazz, final int which) {
        DefaultVestigeSystem currentVestigeSystem = vestigeSystemHolder.getVestigeSystem();
        vestigeSystemHolder.setVestigeSystem(vestigeSystem);
        try {
            if (previousSecurityManager != null) {
                previousSecurityManager.checkMemberAccess(clazz, which);
            }
            SecurityManager securityManager = getSecurityManager();
            if (securityManager != null) {
                securityManager.checkMemberAccess(clazz, which);
            }
        } finally {
            vestigeSystemHolder.setVestigeSystem(currentVestigeSystem);
        }
    }

    @Override
    public void checkMulticast(final InetAddress maddr) {
        DefaultVestigeSystem currentVestigeSystem = vestigeSystemHolder.getVestigeSystem();
        vestigeSystemHolder.setVestigeSystem(vestigeSystem);
        try {
            if (previousSecurityManager != null) {
                previousSecurityManager.checkMulticast(maddr);
            }
            SecurityManager securityManager = getSecurityManager();
            if (securityManager != null) {
                securityManager.checkMulticast(maddr);
            }
        } finally {
            vestigeSystemHolder.setVestigeSystem(currentVestigeSystem);
        }
    }

    @Override
    public void checkMulticast(final InetAddress maddr, final byte ttl) {
        DefaultVestigeSystem currentVestigeSystem = vestigeSystemHolder.getVestigeSystem();
        vestigeSystemHolder.setVestigeSystem(vestigeSystem);
        try {
            if (previousSecurityManager != null) {
                previousSecurityManager.checkMulticast(maddr, ttl);
            }
            SecurityManager securityManager = getSecurityManager();
            if (securityManager != null) {
                securityManager.checkMulticast(maddr, ttl);
            }
        } finally {
            vestigeSystemHolder.setVestigeSystem(currentVestigeSystem);
        }
    }

    @Override
    public void checkPackageAccess(final String pkg) {
        DefaultVestigeSystem currentVestigeSystem = vestigeSystemHolder.getVestigeSystem();
        vestigeSystemHolder.setVestigeSystem(vestigeSystem);
        try {
            if (previousSecurityManager != null) {
                previousSecurityManager.checkPackageAccess(pkg);
            }
            SecurityManager securityManager = getSecurityManager();
            if (securityManager != null) {
                securityManager.checkPackageAccess(pkg);
            }
        } finally {
            vestigeSystemHolder.setVestigeSystem(currentVestigeSystem);
        }
    }

    @Override
    public void checkPackageDefinition(final String pkg) {
        DefaultVestigeSystem currentVestigeSystem = vestigeSystemHolder.getVestigeSystem();
        vestigeSystemHolder.setVestigeSystem(vestigeSystem);
        try {
            if (previousSecurityManager != null) {
                previousSecurityManager.checkPackageDefinition(pkg);
            }
            SecurityManager securityManager = getSecurityManager();
            if (securityManager != null) {
                securityManager.checkPackageDefinition(pkg);
            }
        } finally {
            vestigeSystemHolder.setVestigeSystem(currentVestigeSystem);
        }
    }

    @Override
    public void checkPermission(final Permission perm) {
        DefaultVestigeSystem currentVestigeSystem = vestigeSystemHolder.getVestigeSystem();
        vestigeSystemHolder.setVestigeSystem(vestigeSystem);
        try {
            if (previousSecurityManager != null) {
                previousSecurityManager.checkPermission(perm);
            }
            SecurityManager securityManager = getSecurityManager();
            if (securityManager != null) {
                securityManager.checkPermission(perm);
            }
        } finally {
            vestigeSystemHolder.setVestigeSystem(currentVestigeSystem);
        }
    }

    @Override
    public void checkPermission(final Permission perm, final Object context) {
        DefaultVestigeSystem currentVestigeSystem = vestigeSystemHolder.getVestigeSystem();
        vestigeSystemHolder.setVestigeSystem(vestigeSystem);
        try {
            if (previousSecurityManager != null) {
                previousSecurityManager.checkPermission(perm, context);
            }
            SecurityManager securityManager = getSecurityManager();
            if (securityManager != null) {
                securityManager.checkPermission(perm, context);
            }
        } finally {
            vestigeSystemHolder.setVestigeSystem(currentVestigeSystem);
        }
    }

    @Override
    public void checkPrintJobAccess() {
        DefaultVestigeSystem currentVestigeSystem = vestigeSystemHolder.getVestigeSystem();
        vestigeSystemHolder.setVestigeSystem(vestigeSystem);
        try {
            if (previousSecurityManager != null) {
                previousSecurityManager.checkPrintJobAccess();
            }
            SecurityManager securityManager = getSecurityManager();
            if (securityManager != null) {
                securityManager.checkPrintJobAccess();
            }
        } finally {
            vestigeSystemHolder.setVestigeSystem(currentVestigeSystem);
        }
    }

    @Override
    public void checkPropertiesAccess() {
        DefaultVestigeSystem currentVestigeSystem = vestigeSystemHolder.getVestigeSystem();
        vestigeSystemHolder.setVestigeSystem(vestigeSystem);
        try {
            if (previousSecurityManager != null) {
                previousSecurityManager.checkPropertiesAccess();
            }
            SecurityManager securityManager = getSecurityManager();
            if (securityManager != null) {
                securityManager.checkPropertiesAccess();
            }
        } finally {
            vestigeSystemHolder.setVestigeSystem(currentVestigeSystem);
        }
    }

    @Override
    public void checkPropertyAccess(final String key) {
        DefaultVestigeSystem currentVestigeSystem = vestigeSystemHolder.getVestigeSystem();
        vestigeSystemHolder.setVestigeSystem(vestigeSystem);
        try {
            if (previousSecurityManager != null) {
                previousSecurityManager.checkPropertyAccess(key);
            }
            SecurityManager securityManager = getSecurityManager();
            if (securityManager != null) {
                securityManager.checkPropertyAccess(key);
            }
        } finally {
            vestigeSystemHolder.setVestigeSystem(currentVestigeSystem);
        }
    }

    @Override
    public void checkRead(final FileDescriptor fd) {
        DefaultVestigeSystem currentVestigeSystem = vestigeSystemHolder.getVestigeSystem();
        vestigeSystemHolder.setVestigeSystem(vestigeSystem);
        try {
            if (previousSecurityManager != null) {
                previousSecurityManager.checkRead(fd);
            }
            SecurityManager securityManager = getSecurityManager();
            if (securityManager != null) {
                securityManager.checkRead(fd);
            }
        } finally {
            vestigeSystemHolder.setVestigeSystem(currentVestigeSystem);
        }
    }

    @Override
    public void checkRead(final String file) {
        DefaultVestigeSystem currentVestigeSystem = vestigeSystemHolder.getVestigeSystem();
        vestigeSystemHolder.setVestigeSystem(vestigeSystem);
        try {
            if (previousSecurityManager != null) {
                previousSecurityManager.checkRead(file);
            }
            SecurityManager securityManager = getSecurityManager();
            if (securityManager != null) {
                securityManager.checkRead(file);
            }
        } finally {
            vestigeSystemHolder.setVestigeSystem(currentVestigeSystem);
        }
    }

    @Override
    public void checkRead(final String file, final Object context) {
        DefaultVestigeSystem currentVestigeSystem = vestigeSystemHolder.getVestigeSystem();
        vestigeSystemHolder.setVestigeSystem(vestigeSystem);
        try {
            if (previousSecurityManager != null) {
                previousSecurityManager.checkRead(file, context);
            }
            SecurityManager securityManager = getSecurityManager();
            if (securityManager != null) {
                securityManager.checkRead(file, context);
            }
        } finally {
            vestigeSystemHolder.setVestigeSystem(currentVestigeSystem);
        }
    }

    @Override
    public void checkSecurityAccess(final String target) {
        DefaultVestigeSystem currentVestigeSystem = vestigeSystemHolder.getVestigeSystem();
        vestigeSystemHolder.setVestigeSystem(vestigeSystem);
        try {
            if (previousSecurityManager != null) {
                previousSecurityManager.checkSecurityAccess(target);
            }
            SecurityManager securityManager = getSecurityManager();
            if (securityManager != null) {
                securityManager.checkSecurityAccess(target);
            }
        } finally {
            vestigeSystemHolder.setVestigeSystem(currentVestigeSystem);
        }
    }

    @Override
    public void checkSetFactory() {
        DefaultVestigeSystem currentVestigeSystem = vestigeSystemHolder.getVestigeSystem();
        vestigeSystemHolder.setVestigeSystem(vestigeSystem);
        try {
            if (previousSecurityManager != null) {
                previousSecurityManager.checkSetFactory();
            }
            SecurityManager securityManager = getSecurityManager();
            if (securityManager != null) {
                securityManager.checkSetFactory();
            }
        } finally {
            vestigeSystemHolder.setVestigeSystem(currentVestigeSystem);
        }
    }

    @Override
    public void checkSystemClipboardAccess() {
        DefaultVestigeSystem currentVestigeSystem = vestigeSystemHolder.getVestigeSystem();
        vestigeSystemHolder.setVestigeSystem(vestigeSystem);
        try {
            if (previousSecurityManager != null) {
                previousSecurityManager.checkSystemClipboardAccess();
            }
            SecurityManager securityManager = getSecurityManager();
            if (securityManager != null) {
                securityManager.checkSystemClipboardAccess();
            }
        } finally {
            vestigeSystemHolder.setVestigeSystem(currentVestigeSystem);
        }
    }

    @Override
    public boolean checkTopLevelWindow(final Object window) {
        DefaultVestigeSystem currentVestigeSystem = vestigeSystemHolder.getVestigeSystem();
        vestigeSystemHolder.setVestigeSystem(vestigeSystem);
        try {
            if (previousSecurityManager != null && !previousSecurityManager.checkTopLevelWindow(window)) {
                return false;
            }
            SecurityManager securityManager = getSecurityManager();
            if (securityManager != null && !securityManager.checkTopLevelWindow(window)) {
                return false;
            }
            return true;
        } finally {
            vestigeSystemHolder.setVestigeSystem(currentVestigeSystem);
        }
    }

    @Override
    public void checkWrite(final FileDescriptor fd) {
        DefaultVestigeSystem currentVestigeSystem = vestigeSystemHolder.getVestigeSystem();
        vestigeSystemHolder.setVestigeSystem(vestigeSystem);
        try {
            if (previousSecurityManager != null) {
                previousSecurityManager.checkWrite(fd);
            }
            SecurityManager securityManager = getSecurityManager();
            if (securityManager != null) {
                securityManager.checkWrite(fd);
            }
        } finally {
            vestigeSystemHolder.setVestigeSystem(currentVestigeSystem);
        }
    }

    @Override
    public void checkWrite(final String file) {
        DefaultVestigeSystem currentVestigeSystem = vestigeSystemHolder.getVestigeSystem();
        vestigeSystemHolder.setVestigeSystem(vestigeSystem);
        try {
            if (previousSecurityManager != null) {
                previousSecurityManager.checkWrite(file);
            }
            SecurityManager securityManager = getSecurityManager();
            if (securityManager != null) {
                securityManager.checkWrite(file);
            }
        } finally {
            vestigeSystemHolder.setVestigeSystem(currentVestigeSystem);
        }
    }

}
