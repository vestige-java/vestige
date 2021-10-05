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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author Gael Lalire
 */
public class FileVerificationContext {

    private Iterator<? extends FileVerificationMetadata> fileMetadataIterator;

    private List<FileVerificationMetadata> usedFileVerificationMetadata = new ArrayList<FileVerificationMetadata>();

    private List<FileVerificationMetadata> skippedFileVerificationMetadata = new ArrayList<FileVerificationMetadata>();

    private FileVerificationMetadata current;

    public FileVerificationContext(final Iterator<? extends FileVerificationMetadata> fileMetadataIterator) {
        this.fileMetadataIterator = fileMetadataIterator;
    }

    public boolean hasNext() {
        return fileMetadataIterator.hasNext();
    }

    public FileVerificationMetadata next() {
        if (current != null) {
            usedFileVerificationMetadata.add(current);
            current = null;
        }
        current = fileMetadataIterator.next();
        return current;
    }

    public void skip() {
        skippedFileVerificationMetadata.add(current);
    }

    public FileVerificationMetadata getCurrent() {
        return current;
    }

    public List<FileVerificationMetadata> getSkippedFileVerificationMetadata() {
        return skippedFileVerificationMetadata;
    }

    public List<FileVerificationMetadata> getUsedFileVerificationMetadata() {
        if (current != null) {
            usedFileVerificationMetadata.add(current);
            current = null;
        }
        return usedFileVerificationMetadata;
    }

}
