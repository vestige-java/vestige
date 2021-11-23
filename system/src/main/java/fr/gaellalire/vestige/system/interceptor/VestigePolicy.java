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

package fr.gaellalire.vestige.system.interceptor;

import java.security.AccessController;
import java.security.CodeSource;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Policy;
import java.security.PrivilegedAction;
import java.security.ProtectionDomain;
import java.security.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.gaellalire.vestige.core.StackedHandler;

/**
 * @author Gael Lalire
 */
public abstract class VestigePolicy extends Policy implements StackedHandler<Policy> {

    private static final Logger LOGGER = LoggerFactory.getLogger(VestigePolicy.class);

    private ThreadLocal<Boolean> insideImplies;

    private Policy nextHandler;

    public VestigePolicy(final Policy nextHandler) {
        this.nextHandler = nextHandler;
        insideImplies = new ThreadLocal<Boolean>();
    }

    public abstract Policy getCurrentPolicy();

    @Override
    public Parameters getParameters() {
        Policy currentPolicy = getCurrentPolicy();
        if (currentPolicy == null) {
            return super.getParameters();
        }
        return currentPolicy.getParameters();
    }

    @Override
    public PermissionCollection getPermissions(final CodeSource codesource) {
        Policy currentPolicy = getCurrentPolicy();
        if (currentPolicy == null) {
            return AllPermissionCollection.INSTANCE;
        }
        return getCurrentPolicy().getPermissions(codesource);
    }

    @Override
    public PermissionCollection getPermissions(final ProtectionDomain domain) {
        Policy currentPolicy = getCurrentPolicy();
        if (currentPolicy == null) {
            return super.getPermissions(domain);
        }
        return getCurrentPolicy().getPermissions(domain);
    }

    @Override
    public Provider getProvider() {
        Policy currentPolicy = getCurrentPolicy();
        if (currentPolicy == null) {
            return super.getProvider();
        }
        return getCurrentPolicy().getProvider();
    }

    @Override
    public String getType() {
        Policy currentPolicy = getCurrentPolicy();
        if (currentPolicy == null) {
            return super.getType();
        }
        return getCurrentPolicy().getType();
    }

    @Override
    public boolean implies(final ProtectionDomain domain, final Permission permission) {
        if (insideImplies.get() != null) {
            // a policy has all rights to avoid stack overflow
            return true;
        }
        insideImplies.set(Boolean.TRUE);
        try {
            if (getCurrentPolicy().implies(domain, permission)) {
                return true;
            }
            AccessController.doPrivileged(new PrivilegedAction<Void>() {

                @Override
                public Void run() {
                    LOGGER.warn("Permission {} refused for {}", permission, domain);
                    return null;
                }
            });
            return false;
        } finally {
            insideImplies.remove();
        }
    }

    @Override
    public void refresh() {
        Policy currentPolicy = getCurrentPolicy();
        if (currentPolicy == null) {
            super.refresh();
        } else {
            getCurrentPolicy().refresh();
        }
    }

    @Override
    public String toString() {
        Policy currentPolicy = getCurrentPolicy();
        if (currentPolicy == null) {
            return super.toString();
        }
        return getCurrentPolicy().toString();
    }

    @Override
    public boolean equals(final Object obj) {
        Policy currentPolicy = getCurrentPolicy();
        if (currentPolicy == null) {
            return super.equals(obj);
        }
        return getCurrentPolicy().equals(obj);
    }

    @Override
    public int hashCode() {
        Policy currentPolicy = getCurrentPolicy();
        if (currentPolicy == null) {
            return super.hashCode();
        }
        return getCurrentPolicy().hashCode();
    }

    public Policy getNextHandler() {
        return nextHandler;
    }

    public void setNextHandler(final Policy nextHandler) {
        this.nextHandler = nextHandler;
    }

}
