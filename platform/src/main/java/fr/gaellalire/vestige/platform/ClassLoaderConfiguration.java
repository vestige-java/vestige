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

    private List<FileWithMetadata> beforeUrls;

    private List<FileWithMetadata> afterUrls;

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

    public ClassLoaderConfiguration(final Serializable key, final String name, final boolean attachmentScoped, final List<FileWithMetadata> beforeUrls,
            final List<FileWithMetadata> afterUrls, final List<ClassLoaderConfiguration> dependencies, final List<Integer> paths, final List<List<Integer>> pathIdsList,
            final StringParser pathIdsPositionByResourceName, final StringParser pathIdsPositionByClassName, final JPMSClassLoaderConfiguration moduleConfiguration,
            final JPMSNamedModulesConfiguration namedModulesConfiguration, final boolean mdcIncluded) {
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
        for (FileWithMetadata url : beforeUrls) {
            this.permissions.add(new FilePermission(url.getFile().getPath(), "read"));
        }
        for (FileWithMetadata url : afterUrls) {
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

    public List<FileWithMetadata> getBeforeUrls() {
        return beforeUrls;
    }

    public List<FileWithMetadata> getAfterUrls() {
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
        for (FileWithMetadata url : beforeUrls) {
            if (!url.verify()) {
                result = false;
            }
        }
        for (FileWithMetadata url : afterUrls) {
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

    public FileVerificationMetadata createFileVerificationMetadata(final FileWithMetadata fileWithMetadata) throws IOException {
        PatchFileWithMetadata patch = fileWithMetadata.getPatch();
        PatchFileVerificationMetadata patchFileVerificationMetadata = null;
        if (patch != null) {
            patchFileVerificationMetadata = new PatchFileVerificationMetadata(patch.getFile().length(), patch.createSha512());
        }
        return new FileVerificationMetadata(fileWithMetadata.getFile().length(), fileWithMetadata.createSha512(), patchFileVerificationMetadata);
    }

    public AttachmentVerificationMetadata createVerificationMetadata(final Map<ClassLoaderConfiguration, AttachmentVerificationMetadata> verificationMetadataByClassLoaderConfigurations)
            throws IOException {
        AttachmentVerificationMetadata verificationMetadata = verificationMetadataByClassLoaderConfigurations.get(this);
        if (verificationMetadata != null) {
            return verificationMetadata;
        }
        List<FileVerificationMetadata> beforeFiles = new ArrayList<FileVerificationMetadata>();
        for (FileWithMetadata url : beforeUrls) {
            beforeFiles.add(createFileVerificationMetadata(url));
        }
        List<FileVerificationMetadata> afterFiles = new ArrayList<FileVerificationMetadata>();
        for (FileWithMetadata url : afterUrls) {
            afterFiles.add(createFileVerificationMetadata(url));
        }
        List<AttachmentVerificationMetadata> dependencyVerificationMetadatas = new ArrayList<AttachmentVerificationMetadata>();
        for (ClassLoaderConfiguration dependency : dependencies) {
            dependencyVerificationMetadatas.add(dependency.createVerificationMetadata(verificationMetadataByClassLoaderConfigurations));
        }
        verificationMetadata = new AttachmentVerificationMetadata(dependencyVerificationMetadatas, beforeFiles, afterFiles);
        verificationMetadataByClassLoaderConfigurations.put(this, verificationMetadata);
        return verificationMetadata;
    }

    public AttachmentVerificationMetadata createVerificationMetadata() throws IOException {
        return createVerificationMetadata(new HashMap<ClassLoaderConfiguration, AttachmentVerificationMetadata>());
    }

}
