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

package fr.gaellalire.vestige.resolver.common;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.security.Permission;
import java.util.Collection;

import fr.gaellalire.vestige.core.executor.VestigeWorker;
import fr.gaellalire.vestige.platform.AttachedVestigeClassLoader;
import fr.gaellalire.vestige.platform.AttachmentResult;
import fr.gaellalire.vestige.platform.AttachmentVerificationMetadata;
import fr.gaellalire.vestige.platform.ClassLoaderConfiguration;
import fr.gaellalire.vestige.platform.VestigePlatform;
import fr.gaellalire.vestige.spi.resolver.AttachedClassLoader;
import fr.gaellalire.vestige.spi.resolver.PartiallyVerifiedAttachment;
import fr.gaellalire.vestige.spi.resolver.ResolvedClassLoaderConfiguration;
import fr.gaellalire.vestige.spi.resolver.ResolverException;

/**
 * @author Gael Lalire
 */
public class DefaultResolvedClassLoaderConfiguration implements ResolvedClassLoaderConfiguration {

    private VestigePlatform vestigePlatform;

    private VestigeWorker vestigeWorker;

    private ClassLoaderConfiguration classLoaderConfiguration;

    private boolean firstBeforeParent;

    public DefaultResolvedClassLoaderConfiguration(final VestigePlatform vestigePlatform, final VestigeWorker vestigeWorker,
            final ClassLoaderConfiguration classLoaderConfiguration, final boolean firstBeforeParent) {
        this.vestigePlatform = vestigePlatform;
        this.vestigeWorker = vestigeWorker;
        this.classLoaderConfiguration = classLoaderConfiguration;
    }

    public void saveOtherFields(final ObjectOutputStream internObjectOutputStream) throws IOException {

    }

    @Override
    public void save(final ObjectOutputStream objectOutputStream) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ObjectOutputStream internObjectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
        try {
            internObjectOutputStream.writeBoolean(firstBeforeParent);
            internObjectOutputStream.writeObject(classLoaderConfiguration);
            saveOtherFields(internObjectOutputStream);
        } finally {
            internObjectOutputStream.close();
        }
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        objectOutputStream.writeInt(byteArray.length);
        objectOutputStream.write(byteArray, 0, byteArray.length);
    }

    @Override
    public AttachedClassLoader attach() throws ResolverException, InterruptedException {
        int installerAttach;
        synchronized (vestigePlatform) {
            try {
                installerAttach = vestigePlatform.attach(classLoaderConfiguration, null, vestigeWorker, null);
            } catch (IOException e) {
                throw new ResolverException("Unable to attach", e);
            }
        }
        AttachedVestigeClassLoader attachedVestigeClassLoader = vestigePlatform.getAttachedVestigeClassLoader(installerAttach);
        return new DefaultAttachedClassLoader(vestigePlatform, installerAttach,
                new DefaultAttachableClassLoader(vestigePlatform, attachedVestigeClassLoader.getVestigeClassLoader(), attachedVestigeClassLoader.getVestigeJars()));
    }

    @Override
    public AttachedClassLoader verifiedAttach(final String verificationMetadata) throws ResolverException, InterruptedException {
        int installerAttach;

        synchronized (vestigePlatform) {
            try {
                installerAttach = vestigePlatform.attach(classLoaderConfiguration, AttachmentVerificationMetadata.fromString(verificationMetadata), vestigeWorker, null);
            } catch (IOException e) {
                throw new ResolverException("Unable to attach", e);
            }
        }
        AttachedVestigeClassLoader attachedVestigeClassLoader = vestigePlatform.getAttachedVestigeClassLoader(installerAttach);
        return new DefaultAttachedClassLoader(vestigePlatform, installerAttach,
                new DefaultAttachableClassLoader(vestigePlatform, attachedVestigeClassLoader.getVestigeClassLoader(), attachedVestigeClassLoader.getVestigeJars()));
    }

    @Override
    public Collection<Permission> getPermissions() {
        return classLoaderConfiguration.getPermissions();
    }

    @Override
    public boolean isAttachmentScoped() {
        return classLoaderConfiguration.isAttachmentScoped();
    }

    @Override
    public String toString() {
        return classLoaderConfiguration.toString();
    }

    @Override
    public String createVerificationMetadata() throws ResolverException {
        String verificationMetadata;
        try {
            verificationMetadata = classLoaderConfiguration.createVerificationMetadata().toString();
        } catch (IOException e) {
            throw new ResolverException(e);
        }
        return verificationMetadata;
    }

    @Override
    public PartiallyVerifiedAttachment partiallyVerifiedAttach(final String verificationMetadata) throws ResolverException, InterruptedException {
        int installerAttach;

        final AttachmentResult attachmentResult = new AttachmentResult();
        synchronized (vestigePlatform) {
            try {
                installerAttach = vestigePlatform.attach(classLoaderConfiguration, AttachmentVerificationMetadata.fromString(verificationMetadata), vestigeWorker,
                        attachmentResult);
            } catch (IOException e) {
                throw new ResolverException("Unable to attach", e);
            }
        }
        AttachedVestigeClassLoader attachedVestigeClassLoader = attachmentResult.getAttachedVestigeClassLoader();
        final DefaultAttachedClassLoader defaultAttachedClassLoader = new DefaultAttachedClassLoader(vestigePlatform, installerAttach,
                new DefaultAttachableClassLoader(vestigePlatform, attachedVestigeClassLoader.getVestigeClassLoader(), attachedVestigeClassLoader.getVestigeJars()));
        return new PartiallyVerifiedAttachment() {

            @Override
            public boolean isComplete() {
                return attachmentResult.isComplete();
            }

            @Override
            public String getUsedVerificationMetadata() {
                return attachmentResult.getUsedVerificationMetadata();
            }

            @Override
            public String getRemainingVerificationMetadata() {
                return attachmentResult.getRemainingVerificationMetadata();
            }

            @Override
            public AttachedClassLoader getAttachedClassLoader() {
                return defaultAttachedClassLoader;
            }

        };
    }

}
