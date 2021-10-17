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
import java.util.Deque;
import java.util.Iterator;

/**
 * @author Gael Lalire
 */
public class CompleteMetadataVerifierContext implements VerifierContext {

    private AttachmentVerificationMetadata selectedAttachmentVerificationMetadata;

    private AttachmentVerificationMetadata currentAttachmentVerificationMetadata;

    private Deque<AttachmentVerificationMetadata> attachmentVerificationMetadataStack;

    private Iterator<AttachmentVerificationMetadata> currentDependencies;

    private Deque<Iterator<AttachmentVerificationMetadata>> currentDependenciesStack;

    private Iterator<FileVerificationMetadata> jarIterator;

    private FileVerificationMetadata currentFileVerificationMetadata;

    private AbstractFileVerificationMetadata currentAbstractVerificationMetadata;

    public CompleteMetadataVerifierContext(final AttachmentVerificationMetadata attachmentVerificationMetadata) {
        this.attachmentVerificationMetadataStack = new ArrayDeque<AttachmentVerificationMetadata>();
        currentDependenciesStack = new ArrayDeque<Iterator<AttachmentVerificationMetadata>>();

        currentAttachmentVerificationMetadata = attachmentVerificationMetadata;
        currentDependencies = attachmentVerificationMetadata.getDependencyVerificationMetadata().iterator();
    }

    @Override
    public boolean patch() {
        currentAbstractVerificationMetadata = currentFileVerificationMetadata.getPatchFileVerificationMetadata();
        if (currentAbstractVerificationMetadata == null) {
            return false;
        }
        return true;
    }

    @Override
    public boolean verify(final long size, final String sha512) {
        return currentAbstractVerificationMetadata.getSize() == size && currentAbstractVerificationMetadata.getSha512().equals(sha512);
    }

    @Override
    public boolean nextDependency() {
        if (!currentDependencies.hasNext()) {
            return false;
        }
        selectedAttachmentVerificationMetadata = currentDependencies.next();
        return true;
    }

    @Override
    public boolean nextJar() {
        if (!jarIterator.hasNext()) {
            return false;
        }
        currentFileVerificationMetadata = jarIterator.next();
        currentAbstractVerificationMetadata = currentFileVerificationMetadata;
        return true;
    }

    @Override
    public void pushDependency() {
        attachmentVerificationMetadataStack.push(currentAttachmentVerificationMetadata);
        currentDependenciesStack.push(currentDependencies);

        currentAttachmentVerificationMetadata = selectedAttachmentVerificationMetadata;
        currentDependencies = currentAttachmentVerificationMetadata.getDependencyVerificationMetadata().iterator();
    }

    @Override
    public void popDependency() {
        currentAttachmentVerificationMetadata = attachmentVerificationMetadataStack.pop();
        currentDependencies = currentDependenciesStack.pop();
    }

    @Override
    public void selectBeforeJars() {
        jarIterator = currentAttachmentVerificationMetadata.getBeforeFiles().iterator();
    }

    @Override
    public void selectAfterJars() {
        jarIterator = currentAttachmentVerificationMetadata.getAfterFiles().iterator();
    }

    @Override
    public AttachmentVerificationMetadata getCurrentVerificationMetadata() {
        return currentAttachmentVerificationMetadata;
    }

    @Override
    public AttachmentVerificationMetadata getValidatedCurrentVerificationMetadata() {
        return currentAttachmentVerificationMetadata;
    }

    @Override
    public boolean endOfDependencies() {
        return !currentDependencies.hasNext();
    }

    @Override
    public boolean endOfJars() {
        return !jarIterator.hasNext();
    }

}
