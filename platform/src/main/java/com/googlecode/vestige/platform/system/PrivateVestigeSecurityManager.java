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

package com.googlecode.vestige.platform.system;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Gael Lalire
 */
public class PrivateVestigeSecurityManager extends SecurityManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(PrivateVestigeSecurityManager.class);

    private static final RuntimePermission MODIFY_THREAD_GROUP_PERMISSION = new RuntimePermission("modifyThreadGroup");

    private static final RuntimePermission MODIFY_THREAD_PERMISSION = new RuntimePermission("modifyThread");

    private ThreadLocal<List<ThreadGroup>> threadGroupThreadLocal = new InheritableThreadLocal<List<ThreadGroup>>();

    private ThreadGroup rootGroup;

    public PrivateVestigeSecurityManager() {
        ThreadGroup root = Thread.currentThread().getThreadGroup();
        try {
            while (root.getParent() != null) {
                root = root.getParent();
            }
            rootGroup = root;
        } catch (SecurityException e) {
            LOGGER.debug("Access to root thread group not allowed", e);
        }
    }

    public void setThreadGroups(final List<ThreadGroup> threadGroups) {
        threadGroupThreadLocal.set(threadGroups);
    }

    public void unsetThreadGroups() {
        threadGroupThreadLocal.remove();
    }

    @Override
    public void checkAccess(final Thread t) {
        super.checkAccess(t);
        ThreadGroup otherThreadGroup = t.getThreadGroup();
        if (otherThreadGroup == rootGroup || otherThreadGroup == null) {
            return;
        }
        List<ThreadGroup> threadGroups = threadGroupThreadLocal.get();
        if (threadGroups != null) {
            for (ThreadGroup threadGroup : threadGroups) {
                if (threadGroup != null && threadGroup.parentOf(otherThreadGroup)) {
                    return;
                }
            }
            checkPermission(MODIFY_THREAD_PERMISSION);
        }
    }

    @Override
    public void checkAccess(final ThreadGroup g) {
        super.checkAccess(g);
        if (g == rootGroup) {
            return;
        }
        List<ThreadGroup> threadGroups = threadGroupThreadLocal.get();
        if (threadGroups != null) {
            for (ThreadGroup threadGroup : threadGroups) {
                if (threadGroup != null && threadGroup.parentOf(g)) {
                    return;
                }
            }
            checkPermission(MODIFY_THREAD_GROUP_PERMISSION);
        }
    }

}
