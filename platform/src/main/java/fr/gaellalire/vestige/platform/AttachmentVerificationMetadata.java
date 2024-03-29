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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author Gael Lalire
 */
public class AttachmentVerificationMetadata {

    public static final String METADATA_SEPARATOR = "/";

    public static final String SIGNATURE_TERMINATOR = "\n";

    public static final String FILE_LIST_TERMINATOR = "%";

    public static final String FILE_TERMINATOR = ";";

    public static final String PATCH_SEPARATOR = "!";

    public static final String SIGNATURE_REFERENCE = "@";

    public static final String ATTACHMENT_TERMINATOR = "$";

    private List<AttachmentVerificationMetadata> dependencySignatures;

    private List<FileVerificationMetadata> beforeFiles;

    private List<FileVerificationMetadata> afterFiles;

    private boolean[] beforeUsedArray;

    private boolean[] afterUsedArray;

    /**
     * @author Gael Lalire
     */
    private static class Key {

        private long size;

        private String sha512;

        public Key(final long size, final String sha512) {
            this.size = size;
            this.sha512 = sha512;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + sha512.hashCode();
            result = prime * result + (int) (size ^ (size >>> 32));
            return result;
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof Key)) {
                return false;
            }
            Key other = (Key) obj;
            if (!sha512.equals(other.sha512)) {
                return false;
            }
            if (size != other.size) {
                return false;
            }
            return true;
        }

    }

    private Map<Key, FileMetadataLocation> valueByKey;

    /**
     * @return the current location, or null if not found
     */
    public FileMetadataLocation findAndMarkAsUsed(final long size, final String sha512) {
        FileMetadataLocation value = valueByKey.get(new Key(size, sha512));
        if (value == null) {
            return null;
        }
        if (value.isBefore()) {
            value.getAttachmentVerificationMetadata().beforeUsedArray[value.getPosition()] = true;
        } else {
            value.getAttachmentVerificationMetadata().afterUsedArray[value.getPosition()] = true;
        }
        return value;
    }

    public List<AttachmentVerificationMetadata> extract(final IdentityHashMap<AttachmentVerificationMetadata, List<AttachmentVerificationMetadata>> alreadyExtracted,
            final boolean used) {
        List<FileVerificationMetadata> beforeFiles = new ArrayList<FileVerificationMetadata>();
        List<FileVerificationMetadata> afterFiles = new ArrayList<FileVerificationMetadata>();
        List<AttachmentVerificationMetadata> dependencySignatures = new ArrayList<AttachmentVerificationMetadata>();

        for (AttachmentVerificationMetadata dependencySignature : this.dependencySignatures) {
            List<AttachmentVerificationMetadata> list = alreadyExtracted.get(dependencySignature);
            if (list == null) {
                list = dependencySignature.extract(alreadyExtracted, used);
            }
            for (AttachmentVerificationMetadata attachmentVerificationMetadata : list) {
                if (!dependencySignatures.contains(attachmentVerificationMetadata)) {
                    dependencySignatures.add(attachmentVerificationMetadata);
                }
            }
        }

        Iterator<FileVerificationMetadata> iterator = this.beforeFiles.iterator();
        for (boolean beforeUsed : beforeUsedArray) {
            if (beforeUsed == used) {
                beforeFiles.add(iterator.next());
            }
        }
        iterator = this.afterFiles.iterator();
        for (boolean afterUsed : afterUsedArray) {
            if (afterUsed == used) {
                afterFiles.add(iterator.next());
            }
        }

        List<AttachmentVerificationMetadata> attachmentVerificationMetadatas;
        if (beforeFiles.size() == 0 && afterFiles.size() == 0) {
            attachmentVerificationMetadatas = dependencySignatures;
        } else {
            attachmentVerificationMetadatas = Collections.singletonList(new AttachmentVerificationMetadata(dependencySignatures, beforeFiles, afterFiles));
        }

        alreadyExtracted.put(this, attachmentVerificationMetadatas);

        return attachmentVerificationMetadatas;
    }

    public AttachmentVerificationMetadata extractUsed() {
        List<AttachmentVerificationMetadata> extract = extract(new IdentityHashMap<AttachmentVerificationMetadata, List<AttachmentVerificationMetadata>>(), true);
        if (extract.size() == 1) {
            return extract.get(0);
        } else if (extract.size() == 0) {
            return null;
        }
        return new AttachmentVerificationMetadata(extract, Collections.<FileVerificationMetadata> emptyList(), Collections.<FileVerificationMetadata> emptyList());
    }

    public AttachmentVerificationMetadata extractUnused() {
        List<AttachmentVerificationMetadata> extract = extract(new IdentityHashMap<AttachmentVerificationMetadata, List<AttachmentVerificationMetadata>>(), false);
        if (extract.size() == 1) {
            return extract.get(0);
        } else if (extract.size() == 0) {
            return null;
        }
        return new AttachmentVerificationMetadata(extract, Collections.<FileVerificationMetadata> emptyList(), Collections.<FileVerificationMetadata> emptyList());
    }

    public boolean isPreparedForUseCheck() {
        return valueByKey != null;
    }

    public void prepareForUseCheck() {
        valueByKey = new HashMap<AttachmentVerificationMetadata.Key, FileMetadataLocation>();
        beforeUsedArray = new boolean[beforeFiles.size()];
        afterUsedArray = new boolean[afterFiles.size()];
        for (AttachmentVerificationMetadata dependencySignature : this.dependencySignatures) {
            dependencySignature.prepareForUseCheck();
            valueByKey.putAll(dependencySignature.valueByKey);
        }
        int position = 0;
        for (FileVerificationMetadata file : beforeFiles) {
            valueByKey.put(new Key(file.getSize(), file.getSha512()), new FileMetadataLocation(this, position, true));
            position++;
        }
        position = 0;
        for (FileVerificationMetadata file : afterFiles) {
            valueByKey.put(new Key(file.getSize(), file.getSha512()), new FileMetadataLocation(this, position, false));
            position++;
        }
    }

    public AttachmentVerificationMetadata(final List<AttachmentVerificationMetadata> dependencySignatures, final List<FileVerificationMetadata> beforeFiles,
            final List<FileVerificationMetadata> afterFiles) {
        this.dependencySignatures = dependencySignatures;
        this.beforeFiles = beforeFiles;
        this.afterFiles = afterFiles;
    }

    public List<FileVerificationMetadata> getBeforeFiles() {
        return beforeFiles;
    }

    public List<FileVerificationMetadata> getAfterFiles() {
        return afterFiles;
    }

    public List<AttachmentVerificationMetadata> getDependencyVerificationMetadata() {
        return dependencySignatures;
    }

    public void append(final AbstractFileVerificationMetadata abstractFileVerificationMetadata, final StringBuilder stringBuilder) {
        stringBuilder.append(abstractFileVerificationMetadata.getSize());
        stringBuilder.append(METADATA_SEPARATOR);
        stringBuilder.append(abstractFileVerificationMetadata.getSha512());
    }

    public void append(final int depth, final List<AttachmentVerificationMetadata> verificationMetadatas, final StringBuilder stringBuilder) {
        int indexOf = verificationMetadatas.indexOf(this);
        for (int i = 0; i < depth; i++) {
            stringBuilder.append(" ");
        }
        if (indexOf != -1) {
            // do not repeat same data
            stringBuilder.append(SIGNATURE_REFERENCE);
            stringBuilder.append(indexOf);
            stringBuilder.append(SIGNATURE_TERMINATOR);
        } else {
            verificationMetadatas.add(this);
            for (FileVerificationMetadata url : beforeFiles) {
                append(url, stringBuilder);
                PatchFileVerificationMetadata patchFileVerificationMetadata = url.getPatchFileVerificationMetadata();
                if (patchFileVerificationMetadata != null) {
                    stringBuilder.append(PATCH_SEPARATOR);
                    append(patchFileVerificationMetadata, stringBuilder);
                }
                stringBuilder.append(FILE_TERMINATOR);
            }
            stringBuilder.append(FILE_LIST_TERMINATOR);
            for (FileVerificationMetadata url : afterFiles) {
                append(url, stringBuilder);
                PatchFileVerificationMetadata patchFileVerificationMetadata = url.getPatchFileVerificationMetadata();
                if (patchFileVerificationMetadata != null) {
                    stringBuilder.append(PATCH_SEPARATOR);
                    append(patchFileVerificationMetadata, stringBuilder);
                }
                stringBuilder.append(FILE_TERMINATOR);
            }
            stringBuilder.append(SIGNATURE_TERMINATOR);
            for (AttachmentVerificationMetadata dependency : dependencySignatures) {
                dependency.append(depth + 1, verificationMetadatas, stringBuilder);
            }
        }
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        append(0, new ArrayList<AttachmentVerificationMetadata>(), stringBuilder);
        stringBuilder.append(ATTACHMENT_TERMINATOR);
        return stringBuilder.toString();
    }

    public static void parseFileVerificationMetadataList(final String s, final List<FileVerificationMetadata> fileVerificationMetadatas) {
        for (String beforeFile : s.split(FILE_TERMINATOR)) {
            if (beforeFile.length() == 0) {
                continue;
            }
            String[] split = beforeFile.split(PATCH_SEPARATOR);
            PatchFileVerificationMetadata patchFileVerificationMetadata = null;
            if (split.length > 1) {
                String patchMetadataString = split[1];
                int separatorIndex = patchMetadataString.indexOf(METADATA_SEPARATOR);
                patchFileVerificationMetadata = new PatchFileVerificationMetadata(Long.parseLong(patchMetadataString.substring(0, separatorIndex)),
                        patchMetadataString.substring(separatorIndex + 1));
            }
            String fileMetadataString = split[0];
            int separatorIndex = fileMetadataString.indexOf(METADATA_SEPARATOR);
            fileVerificationMetadatas.add(new FileVerificationMetadata(Long.parseLong(fileMetadataString.substring(0, separatorIndex)),
                    fileMetadataString.substring(separatorIndex + 1), patchFileVerificationMetadata));
        }
    }

    public static AttachmentVerificationMetadata fromString(final String s) {
        BufferedReader stringReader = new BufferedReader(new StringReader(s));
        try {
            List<AttachmentVerificationMetadata> signatures = new ArrayList<AttachmentVerificationMetadata>();
            List<List<AttachmentVerificationMetadata>> dependenciesByLevel = new ArrayList<List<AttachmentVerificationMetadata>>();
            int previousLevel = -1;
            String readLine = stringReader.readLine();
            while (readLine != null) {
                int currentLevel = 0;
                while (readLine.charAt(currentLevel) == ' ') {
                    currentLevel++;
                }
                readLine = readLine.substring(currentLevel);
                AttachmentVerificationMetadata currentSignature;
                if (ATTACHMENT_TERMINATOR.equals(readLine)) {
                    break;
                } else if (readLine.startsWith(SIGNATURE_REFERENCE)) {
                    currentSignature = signatures.get(Integer.parseInt(readLine.substring(1)));
                    if (previousLevel > currentLevel) {
                        dependenciesByLevel.subList(currentLevel + 1, dependenciesByLevel.size()).clear();
                    } else if (previousLevel < currentLevel) {
                        dependenciesByLevel.add(null);
                    }
                } else {
                    List<FileVerificationMetadata> currentBeforeFiles = new ArrayList<FileVerificationMetadata>();
                    List<FileVerificationMetadata> currentAfterFiles = new ArrayList<FileVerificationMetadata>();
                    String[] split = readLine.split(FILE_LIST_TERMINATOR);
                    if (split.length == 0) {
                        split = new String[] {"", ""};
                    }
                    parseFileVerificationMetadataList(split[0], currentBeforeFiles);
                    parseFileVerificationMetadataList(split[1], currentAfterFiles);

                    List<AttachmentVerificationMetadata> currentDependencySignatures = new ArrayList<AttachmentVerificationMetadata>();
                    if (previousLevel == currentLevel) {
                        dependenciesByLevel.set(currentLevel, currentDependencySignatures);
                    } else if (previousLevel < currentLevel) {
                        dependenciesByLevel.add(currentDependencySignatures);
                    } else {
                        dependenciesByLevel.set(currentLevel, currentDependencySignatures);
                        dependenciesByLevel.subList(currentLevel + 1, dependenciesByLevel.size()).clear();
                    }
                    currentSignature = new AttachmentVerificationMetadata(currentDependencySignatures, currentBeforeFiles, currentAfterFiles);
                    signatures.add(currentSignature);
                }
                if (currentLevel != 0) {
                    dependenciesByLevel.get(currentLevel - 1).add(currentSignature);
                }

                previousLevel = currentLevel;
                readLine = stringReader.readLine();
            }
            return signatures.get(0);
        } catch (IOException e) {
            // impossible
            return null;
        }

    }

}
