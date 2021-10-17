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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * @author Gael Lalire
 */
public class PartialJarVerifierContext implements VerifierContext {

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
    public AttachmentVerificationMetadata getValidatedCurrentVerificationMetadata() {
        if (validatedAttachmentVerificationMetadata == null) {
            validatedAttachmentVerificationMetadata = new AttachmentVerificationMetadata(validatedDependencies, validatedBeforeFiles, validatedAfterFiles);
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
