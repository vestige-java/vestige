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

import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.List;

/**
 * @author Gael Lalire
 * @param <RuntimeContext> type of runtime context
 */
public class AttachmentContext<RuntimeContext> implements Serializable, Cloneable {

    private static final long serialVersionUID = -7961011280934360532L;

    private PermissionSetFactory permissions;

    private String className;

    private boolean privateSystem;

    private List<AddInject> addInjects;

    private VerificationMetadata verificationMetadata;

    private transient ApplicationResolvedClassLoaderConfiguration resolve;

    private transient WeakReference<RuntimeContext> runtimeApplicationContext;

    public PermissionSetFactory getPermissions() {
        return permissions;
    }

    public ApplicationResolvedClassLoaderConfiguration getResolve() {
        return resolve;
    }

    public String getClassName() {
        return className;
    }

    public boolean isPrivateSystem() {
        return privateSystem;
    }

    public List<AddInject> getAddInjects() {
        return addInjects;
    }

    public RuntimeContext getRuntimeApplicationContext() {
        if (runtimeApplicationContext == null) {
            return null;
        }
        return runtimeApplicationContext.get();
    }

    public void setRuntimeApplicationContext(final RuntimeContext runtimeApplicationContext) {
        this.runtimeApplicationContext = new WeakReference<RuntimeContext>(runtimeApplicationContext);
    }

    public void setPermissions(final PermissionSetFactory permissions) {
        this.permissions = permissions;
    }

    public void setClassName(final String className) {
        this.className = className;
    }

    public void setPrivateSystem(final boolean privateSystem) {
        this.privateSystem = privateSystem;
    }

    public void setAddInjects(final List<AddInject> addInjects) {
        this.addInjects = addInjects;
    }

    public VerificationMetadata getVerificationMetadata() {
        return verificationMetadata;
    }

    public void setVerificationMetadata(final VerificationMetadata verificationMetadata) {
        this.verificationMetadata = verificationMetadata;
    }

    public void setResolve(final ApplicationResolvedClassLoaderConfiguration resolve) {
        this.resolve = resolve;
    }

    @SuppressWarnings("unchecked")
    public AttachmentContext<RuntimeContext> copy() {
        try {
            return (AttachmentContext<RuntimeContext>) clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

}
