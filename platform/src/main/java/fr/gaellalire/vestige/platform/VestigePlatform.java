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

import java.io.IOException;
import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Set;

import fr.gaellalire.vestige.core.VestigeClassLoader;
import fr.gaellalire.vestige.core.executor.VestigeWorker;
import fr.gaellalire.vestige.jpms.ModuleLayerLinker;

/**
 * @author Gael Lalire
 */
public interface VestigePlatform extends ModuleLayerLinker<VestigeClassLoader<AttachedVestigeClassLoader>> {

    int attach(VestigeClassLoader<AttachedVestigeClassLoader> classLoader);

    int attach(ClassLoaderConfiguration classLoaderConfiguration, AttachmentVerificationMetadata verificationMetadata, VestigeWorker vestigeWorker,
            AttachmentResult attachmentResult) throws InterruptedException, IOException;

    void detach(int id);

    List<Serializable> getClassLoaderKeys();

    AttachedVestigeClassLoader getAttachedVestigeClassLoaderByKey(Serializable key);

    Set<Integer> getAttachments();

    VestigeClassLoader<AttachedVestigeClassLoader> getClassLoader(int id);

    AttachedVestigeClassLoader getAttachedVestigeClassLoader(int id);

    List<List<WeakReference<AttachedVestigeClassLoader>>> getAttachmentScopedAttachedClassLoaders();

    List<WeakReference<AttachedVestigeClassLoader>> getAttachmentScopedUnattachedVestigeClassLoaders();

    void discardUnattached();

    void close();

}
