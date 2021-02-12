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
import java.util.List;

/**
 * @author Gael Lalire
 */
public class AttachmentVerificationMetadata {

    public static final String METADATA_SEPARATOR = "/";

    public static final String SIGNATURE_TERMINATOR = "\n";

    public static final String FILE_LIST_TERMINATOR = "%";

    public static final String FILE_TERMINATOR = ";";

    public static final String SIGNATURE_REFERENCE = "@";

    public static final String ATTACHMENT_TERMINATOR = "$";

    private List<AttachmentVerificationMetadata> dependencySignatures;

    private List<FileVerificationMetadata> beforeFiles;

    private List<FileVerificationMetadata> afterFiles;

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
                stringBuilder.append(url.getSize());
                stringBuilder.append(METADATA_SEPARATOR);
                stringBuilder.append(url.getSha512());
                stringBuilder.append(FILE_TERMINATOR);
            }
            stringBuilder.append(FILE_LIST_TERMINATOR);
            for (FileVerificationMetadata url : afterFiles) {
                stringBuilder.append(url.getSize());
                stringBuilder.append(METADATA_SEPARATOR);
                stringBuilder.append(url.getSha512());
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
                    }
                } else {
                    List<FileVerificationMetadata> currentBeforeFiles = new ArrayList<FileVerificationMetadata>();
                    List<FileVerificationMetadata> currentAfterFiles = new ArrayList<FileVerificationMetadata>();
                    String[] split = readLine.split(FILE_LIST_TERMINATOR);
                    String before = split[0];
                    for (String beforeFile : before.split(FILE_TERMINATOR)) {
                        if (beforeFile.length() == 0) {
                            continue;
                        }
                        int separatorIndex = beforeFile.indexOf(METADATA_SEPARATOR);
                        currentBeforeFiles.add(new FileVerificationMetadata(Long.parseLong(beforeFile.substring(0, separatorIndex)), beforeFile.substring(separatorIndex + 1)));
                    }
                    String after = split[1];
                    for (String afterFile : after.split(FILE_TERMINATOR)) {
                        if (afterFile.length() == 0) {
                            continue;
                        }
                        int separatorIndex = afterFile.indexOf(METADATA_SEPARATOR);
                        currentAfterFiles.add(new FileVerificationMetadata(Long.parseLong(afterFile.substring(0, separatorIndex)), afterFile.substring(separatorIndex + 1)));
                    }
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
