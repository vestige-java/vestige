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

/**
 * @author Gael Lalire
 */
public class AttachmentResult {

    private String usedVerificationMetadata;

    private String remainingVerificationMetadata;

    private AttachedVestigeClassLoader attachedVestigeClassLoader;

    private boolean complete;

    public String getUsedVerificationMetadata() {
        return usedVerificationMetadata;
    }

    public void setUsedVerificationMetadata(final String usedVerificationMetadata) {
        this.usedVerificationMetadata = usedVerificationMetadata;
    }

    public String getRemainingVerificationMetadata() {
        return remainingVerificationMetadata;
    }

    public void setRemainingVerificationMetadata(final String remainingVerificationMetadata) {
        this.remainingVerificationMetadata = remainingVerificationMetadata;
    }

    public AttachedVestigeClassLoader getAttachedVestigeClassLoader() {
        return attachedVestigeClassLoader;
    }

    public void setAttachedVestigeClassLoader(final AttachedVestigeClassLoader attachedVestigeClassLoader) {
        this.attachedVestigeClassLoader = attachedVestigeClassLoader;
    }

    public boolean isComplete() {
        return complete;
    }

    public void setComplete(final boolean complete) {
        this.complete = complete;
    }

}
