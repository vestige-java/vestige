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

package com.googlecode.vestige.platform.system.interceptor;

import java.io.FileDescriptor;
import java.net.InetAddress;
import java.security.Permission;

import com.googlecode.vestige.core.StackedHandler;

/**
 * @author Gael Lalire
 */
public abstract class VestigeSecurityManager extends SecurityManager implements StackedHandler<SecurityManager> {

    private SecurityManager nextHandler;

    public VestigeSecurityManager(final SecurityManager nextHandler, final SecurityManager previousSecurityManager) {
        this.nextHandler = nextHandler;
    }

    public abstract SecurityManager getSecurityManager();

    @Override
    public void checkAccept(final String host, final int port) {
        getSecurityManager().checkAccept(host, port);
    }

    @Override
    public void checkAccess(final Thread t) {
        getSecurityManager().checkAccess(t);
    }

    @Override
    public void checkAccess(final ThreadGroup g) {
        getSecurityManager().checkAccess(g);
    }

    @Override
    public void checkAwtEventQueueAccess() {
        getSecurityManager().checkAwtEventQueueAccess();
    }

    @Override
    public void checkConnect(final String host, final int port) {
        getSecurityManager().checkConnect(host, port);
    }

    @Override
    public void checkConnect(final String host, final int port, final Object context) {
        getSecurityManager().checkConnect(host, port, context);
    }

    @Override
    public void checkCreateClassLoader() {
        getSecurityManager().checkCreateClassLoader();
    }

    @Override
    public void checkDelete(final String file) {
        getSecurityManager().checkDelete(file);
    }

    @Override
    public void checkExec(final String cmd) {
        getSecurityManager().checkExec(cmd);
    }

    @Override
    public void checkExit(final int status) {
        getSecurityManager().checkExit(status);
    }

    @Override
    public void checkLink(final String lib) {
        getSecurityManager().checkLink(lib);
    }

    @Override
    public void checkListen(final int port) {
        getSecurityManager().checkListen(port);
    }

    @Override
    public void checkMemberAccess(final Class<?> clazz, final int which) {
        getSecurityManager().checkMemberAccess(clazz, which);
    }

    @Override
    public void checkMulticast(final InetAddress maddr) {
        getSecurityManager().checkMulticast(maddr);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void checkMulticast(final InetAddress maddr, final byte ttl) {
        getSecurityManager().checkMulticast(maddr, ttl);
    }

    @Override
    public void checkPackageAccess(final String pkg) {
        getSecurityManager().checkPackageAccess(pkg);
    }

    @Override
    public void checkPackageDefinition(final String pkg) {
        getSecurityManager().checkPackageDefinition(pkg);
    }

    @Override
    public void checkPermission(final Permission perm) {
        getSecurityManager().checkPermission(perm);
    }

    @Override
    public void checkPermission(final Permission perm, final Object context) {
        getSecurityManager().checkPermission(perm, context);
    }

    @Override
    public void checkPrintJobAccess() {
        getSecurityManager().checkPrintJobAccess();
    }

    @Override
    public void checkPropertiesAccess() {
        getSecurityManager().checkPropertiesAccess();
    }

    @Override
    public void checkPropertyAccess(final String key) {
        getSecurityManager().checkPropertyAccess(key);
    }

    @Override
    public void checkRead(final FileDescriptor fd) {
        getSecurityManager().checkRead(fd);
    }

    @Override
    public void checkRead(final String file) {
        getSecurityManager().checkRead(file);
    }

    @Override
    public void checkRead(final String file, final Object context) {
        getSecurityManager().checkRead(file, context);
    }

    @Override
    public void checkSecurityAccess(final String target) {
        getSecurityManager().checkSecurityAccess(target);
    }

    @Override
    public void checkSetFactory() {
        getSecurityManager().checkSetFactory();
    }

    @Override
    public void checkSystemClipboardAccess() {
        getSecurityManager().checkSystemClipboardAccess();
    }

    @Override
    public boolean checkTopLevelWindow(final Object window) {
        return getSecurityManager().checkTopLevelWindow(window);
    }

    @Override
    public void checkWrite(final FileDescriptor fd) {
        getSecurityManager().checkWrite(fd);
    }

    @Override
    public void checkWrite(final String file) {
        getSecurityManager().checkWrite(file);
    }

    @Override
    public boolean equals(final Object obj) {
        return getSecurityManager().equals(obj);
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean getInCheck() {
        return getSecurityManager().getInCheck();
    }

    @Override
    public Object getSecurityContext() {
        return getSecurityManager().getSecurityContext();
    }

    @Override
    public ThreadGroup getThreadGroup() {
        return getSecurityManager().getThreadGroup();
    }

    @Override
    public int hashCode() {
        return getSecurityManager().hashCode();
    }

    @Override
    public String toString() {
        return getSecurityManager().toString();
    }

    public SecurityManager getNextHandler() {
        return nextHandler;
    }

    public void setNextHandler(final SecurityManager nextHandler) {
        this.nextHandler = nextHandler;
    }

}
