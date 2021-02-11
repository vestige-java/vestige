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

package fr.gaellalire.vestige.platform;

import java.io.FilePermission;
import java.io.IOException;
import java.io.Serializable;
import java.security.Permission;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import fr.gaellalire.vestige.core.parser.StringParser;

/**
 * @author Gael Lalire
 */
public class ClassLoaderConfiguration implements Serializable {

    private static final long serialVersionUID = 4540499333086383595L;

    private Serializable key;

    private List<SecureFile> beforeUrls;

    private List<SecureFile> afterUrls;

    private List<ClassLoaderConfiguration> dependencies;

    private List<Integer> paths;

    private List<List<Integer>> pathIdsList;

    private StringParser pathIdsPositionByResourceName;

    private StringParser pathIdsPositionByClassName;

    private boolean attachmentScoped;

    private String name;

    private Set<Permission> permissions;

    private JPMSClassLoaderConfiguration moduleConfiguration;

    private JPMSNamedModulesConfiguration namedModulesConfiguration;

    private boolean mdcIncluded;

    public ClassLoaderConfiguration(final Serializable key, final String name, final boolean attachmentScoped, final List<SecureFile> beforeUrls, final List<SecureFile> afterUrls,
            final List<ClassLoaderConfiguration> dependencies, final List<Integer> paths, final List<List<Integer>> pathIdsList, final StringParser pathIdsPositionByResourceName,
            final StringParser pathIdsPositionByClassName, final JPMSClassLoaderConfiguration moduleConfiguration, final JPMSNamedModulesConfiguration namedModulesConfiguration,
            final boolean mdcIncluded) {
        this.key = key;
        this.name = name;
        this.attachmentScoped = attachmentScoped;
        this.beforeUrls = beforeUrls;
        this.afterUrls = afterUrls;
        this.dependencies = dependencies;
        this.paths = paths;
        this.pathIdsList = pathIdsList;
        this.pathIdsPositionByResourceName = pathIdsPositionByResourceName;
        this.pathIdsPositionByClassName = pathIdsPositionByClassName;
        this.permissions = new HashSet<Permission>();
        for (SecureFile url : beforeUrls) {
            this.permissions.add(new FilePermission(url.getFile().getPath(), "read"));
        }
        for (SecureFile url : afterUrls) {
            this.permissions.add(new FilePermission(url.getFile().getPath(), "read"));
        }
        for (ClassLoaderConfiguration dependency : dependencies) {
            this.permissions.addAll(dependency.getPermissions());
        }
        this.moduleConfiguration = moduleConfiguration;
        this.namedModulesConfiguration = namedModulesConfiguration;
        this.mdcIncluded = mdcIncluded;
    }

    public JPMSNamedModulesConfiguration getNamedModulesConfiguration() {
        return namedModulesConfiguration;
    }

    public int getDependencyIndex(final int pathIndex) {
        return paths.get(pathIndex * 2).intValue();
    }

    public int getDependencyPathIndex(final int pathIndex) {
        return paths.get(pathIndex * 2 + 1).intValue();
    }

    public Serializable getKey() {
        return key;
    }

    public String getName() {
        return name;
    }

    public List<SecureFile> getBeforeUrls() {
        return beforeUrls;
    }

    public List<SecureFile> getAfterUrls() {
        return afterUrls;
    }

    public List<ClassLoaderConfiguration> getDependencies() {
        return dependencies;
    }

    public List<List<Integer>> getPathIdsList() {
        return pathIdsList;
    }

    public StringParser getPathIdsPositionByResourceName() {
        return pathIdsPositionByResourceName;
    }

    public StringParser getPathIdsPositionByClassName() {
        return pathIdsPositionByClassName;
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof ClassLoaderConfiguration)) {
            return false;
        }
        ClassLoaderConfiguration other = (ClassLoaderConfiguration) obj;
        return key.equals(other.getKey());
    }

    public boolean isAttachmentScoped() {
        return attachmentScoped;
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }

    @Override
    public String toString() {
        return key.toString();
    }

    public Set<Permission> getPermissions() {
        return permissions;
    }

    public JPMSClassLoaderConfiguration getModuleConfiguration() {
        if (moduleConfiguration == null) {
            moduleConfiguration = JPMSClassLoaderConfiguration.EMPTY_INSTANCE;
        }
        return moduleConfiguration;
    }

    public boolean verify() {
        boolean result = true;
        for (SecureFile url : beforeUrls) {
            if (!url.verify()) {
                result = false;
            }
        }
        for (SecureFile url : afterUrls) {
            if (!url.verify()) {
                result = false;
            }
        }
        for (ClassLoaderConfiguration dependency : dependencies) {
            if (!dependency.verify()) {
                result = false;
            }
        }
        return result;
    }

    public boolean isMdcIncluded() {
        return mdcIncluded;
    }

    public AttachmentVerificationMetadata createSignature(final Map<ClassLoaderConfiguration, AttachmentVerificationMetadata> signatureByClassLoaderConfigurations) throws IOException {
        AttachmentVerificationMetadata signature = signatureByClassLoaderConfigurations.get(this);
        if (signature != null) {
            return signature;
        }
        List<FileVerificationMetadata> beforeFiles = new ArrayList<FileVerificationMetadata>();
        for (SecureFile url : beforeUrls) {
            beforeFiles.add(new FileVerificationMetadata(url.getFile().length(), url.createSha512()));
        }
        List<FileVerificationMetadata> afterFiles = new ArrayList<FileVerificationMetadata>();
        for (SecureFile url : afterUrls) {
            afterFiles.add(new FileVerificationMetadata(url.getFile().length(), url.createSha512()));
        }
        List<AttachmentVerificationMetadata> dependencySignatures = new ArrayList<AttachmentVerificationMetadata>();
        for (ClassLoaderConfiguration dependency : dependencies) {
            dependencySignatures.add(dependency.createSignature(signatureByClassLoaderConfigurations));
        }
        signature = new AttachmentVerificationMetadata(dependencySignatures, beforeFiles, afterFiles);
        signatureByClassLoaderConfigurations.put(this, signature);
        return signature;
    }

    public AttachmentVerificationMetadata createSignature() throws IOException {
        return createSignature(new HashMap<ClassLoaderConfiguration, AttachmentVerificationMetadata>());
    }

}
