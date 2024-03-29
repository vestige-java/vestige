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

import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author Gael Lalire
 */
public class PartialJarVerifierContext implements VerifierContext {

    private Map<Serializable, AttachmentVerificationMetadata> verificationMetadataByClassLoaderConfigurationKey;

    private AttachmentVerificationMetadata attachmentVerificationMetadata;

    private FileMetadataLocation currentFileMetadataLocation;

    private AbstractFileVerificationMetadata abstractFileVerificationMetadata;

    private Deque<List<AttachmentVerificationMetadata>> validatedDependenciesStack;

    private List<AttachmentVerificationMetadata> validatedDependencies;

    private List<FileVerificationMetadata> validatedAfterFiles;

    private List<FileVerificationMetadata> validatedBeforeFiles;

    private List<FileVerificationMetadata> validatedFiles;

    private AttachmentVerificationMetadata validatedAttachmentVerificationMetadata;

    public PartialJarVerifierContext(final AttachmentVerificationMetadata attachmentVerificationMetadata) {
        this.attachmentVerificationMetadata = attachmentVerificationMetadata;
        validatedDependenciesStack = new ArrayDeque<List<AttachmentVerificationMetadata>>();
        validatedDependencies = new ArrayList<AttachmentVerificationMetadata>();
        verificationMetadataByClassLoaderConfigurationKey = new HashMap<Serializable, AttachmentVerificationMetadata>();
    }

    @Override
    public boolean verify(final long size, final String sha512) {
        if (abstractFileVerificationMetadata != null) {
            AbstractFileVerificationMetadata abstractFileVerificationMetadata = this.abstractFileVerificationMetadata;
            this.abstractFileVerificationMetadata = null;
            FileVerificationMetadata remove = validatedFiles.remove(validatedFiles.size() - 1);
            validatedFiles.add(new FileVerificationMetadata(remove.getSize(), remove.getSha512(), new PatchFileVerificationMetadata(size, sha512)));
            return abstractFileVerificationMetadata.getSize() == size && abstractFileVerificationMetadata.getSha512().equals(sha512);
        }

        currentFileMetadataLocation = attachmentVerificationMetadata.findAndMarkAsUsed(size, sha512);
        if (currentFileMetadataLocation == null) {
            return false;
        }
        validatedFiles.add(new FileVerificationMetadata(size, sha512, null));
        return true;
    }

    @Override
    public boolean nextDependency() {
        return true;
    }

    @Override
    public boolean patch() {
        if (currentFileMetadataLocation == null) {
            return false;
        }
        AttachmentVerificationMetadata currentAttachmentVerificationMetadata = currentFileMetadataLocation.getAttachmentVerificationMetadata();
        List<FileVerificationMetadata> files;
        if (currentFileMetadataLocation.isBefore()) {
            files = currentAttachmentVerificationMetadata.getBeforeFiles();
        } else {
            files = currentAttachmentVerificationMetadata.getAfterFiles();
        }
        FileVerificationMetadata fileVerificationMetadata = files.get(currentFileMetadataLocation.getPosition());
        abstractFileVerificationMetadata = fileVerificationMetadata.getPatchFileVerificationMetadata();
        if (abstractFileVerificationMetadata == null) {
            return false;
        }
        return true;
    }

    @Override
    public void pushDependency() {
        validatedDependenciesStack.push(validatedDependencies);
        validatedDependencies = new ArrayList<AttachmentVerificationMetadata>();
    }

    public AttachmentVerificationMetadata generateAttachmentVerificationMetadata(final ClassLoaderConfiguration classLoaderConfiguration,
            final AttachmentVerificationMetadata unsharedAttachmentVerificationMetadata) {
        Serializable key = classLoaderConfiguration.getKey();
        AttachmentVerificationMetadata sharedAttachmentVerificationMetadata = verificationMetadataByClassLoaderConfigurationKey.get(key);
        if (sharedAttachmentVerificationMetadata != null) {
            // already loaded
            return sharedAttachmentVerificationMetadata;
        }
        List<AttachmentVerificationMetadata> dependencyAttachmentVerificationMetadatas = new ArrayList<AttachmentVerificationMetadata>();
        List<ClassLoaderConfiguration> dependencies = classLoaderConfiguration.getDependencies();
        Iterator<AttachmentVerificationMetadata> iterator = unsharedAttachmentVerificationMetadata.getDependencyVerificationMetadata().iterator();
        for (ClassLoaderConfiguration subClassLoaderConfiguration : dependencies) {
            dependencyAttachmentVerificationMetadatas.add(generateAttachmentVerificationMetadata(subClassLoaderConfiguration, iterator.next()));
        }

        List<FileVerificationMetadata> beforeFiles = unsharedAttachmentVerificationMetadata.getBeforeFiles();
        for (FileVerificationMetadata fileVerificationMetadata : beforeFiles) {
            attachmentVerificationMetadata.findAndMarkAsUsed(fileVerificationMetadata.getSize(), fileVerificationMetadata.getSha512());
        }
        List<FileVerificationMetadata> afterFiles = unsharedAttachmentVerificationMetadata.getAfterFiles();
        for (FileVerificationMetadata fileVerificationMetadata : afterFiles) {
            attachmentVerificationMetadata.findAndMarkAsUsed(fileVerificationMetadata.getSize(), fileVerificationMetadata.getSha512());
        }
        sharedAttachmentVerificationMetadata = new AttachmentVerificationMetadata(dependencyAttachmentVerificationMetadatas, beforeFiles, afterFiles);
        verificationMetadataByClassLoaderConfigurationKey.put(key, sharedAttachmentVerificationMetadata);
        return sharedAttachmentVerificationMetadata;
    }

    @Override
    public void useCachedClassLoader(final ClassLoaderConfiguration classLoaderConfiguration, final String verifiedMetada) {
        Serializable classLoaderConfigurationKey = classLoaderConfiguration.getKey();
        validatedAttachmentVerificationMetadata = verificationMetadataByClassLoaderConfigurationKey.get(classLoaderConfigurationKey);
        if (validatedAttachmentVerificationMetadata == null) {
            // The dependency is loaded by another attachment, some sub-dependencies may be already loaded by our attachment
            // need to browse each dependency of classLoaderConfiguration
            validatedAttachmentVerificationMetadata = generateAttachmentVerificationMetadata(classLoaderConfiguration, AttachmentVerificationMetadata.fromString(verifiedMetada));
        }
    }

    @Override
    public void popDependency() {
        List<AttachmentVerificationMetadata> pop = validatedDependenciesStack.pop();
        if (validatedAttachmentVerificationMetadata == null) {
            validatedAttachmentVerificationMetadata = new AttachmentVerificationMetadata(validatedDependencies, validatedBeforeFiles, validatedAfterFiles);
        }
        pop.add(validatedAttachmentVerificationMetadata);
        validatedAttachmentVerificationMetadata = null;
        validatedDependencies = pop;
    }

    @Override
    public void selectBeforeJars() {
        validatedFiles = new ArrayList<FileVerificationMetadata>();
        validatedBeforeFiles = validatedFiles;
    }

    @Override
    public void selectAfterJars() {
        validatedFiles = new ArrayList<FileVerificationMetadata>();
        validatedAfterFiles = validatedFiles;
    }

    @Override
    public boolean nextJar() {
        return true;
    }

    @Override
    public AttachmentVerificationMetadata getCurrentVerificationMetadata() {
        return attachmentVerificationMetadata;
    }

    @Override
    public AttachmentVerificationMetadata getValidatedCurrentVerificationMetadata(final ClassLoaderConfiguration classLoaderConfiguration) {
        if (validatedAttachmentVerificationMetadata == null) {
            Serializable classLoaderConfigurationKey = classLoaderConfiguration.getKey();
            validatedAttachmentVerificationMetadata = verificationMetadataByClassLoaderConfigurationKey.get(classLoaderConfigurationKey);
            if (validatedAttachmentVerificationMetadata == null) {
                validatedAttachmentVerificationMetadata = new AttachmentVerificationMetadata(validatedDependencies, validatedBeforeFiles, validatedAfterFiles);
                verificationMetadataByClassLoaderConfigurationKey.put(classLoaderConfigurationKey, validatedAttachmentVerificationMetadata);
            }
        }
        return validatedAttachmentVerificationMetadata;
    }

    @Override
    public boolean endOfDependencies() {
        return true;
    }

    @Override
    public boolean endOfJars() {
        return true;
    }

}
