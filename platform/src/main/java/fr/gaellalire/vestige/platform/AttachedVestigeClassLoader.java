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

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.List;

import fr.gaellalire.vestige.core.VestigeClassLoader;
import fr.gaellalire.vestige.core.resource.VestigeResourceLocator;
import fr.gaellalire.vestige.jpms.JPMSInRepositoryModuleLayerAccessor;
import fr.gaellalire.vestige.spi.resolver.VestigeJar;

/**
 * @author Gael Lalire
 */
public class AttachedVestigeClassLoader {

    private List<SoftReference<?>> softReferences;

    private VestigeClassLoader<AttachedVestigeClassLoader> vestigeClassLoader;

    private List<AttachedVestigeClassLoader> dependencies;

    private int attachments;

    private String name;

    private boolean attachmentScoped;

    private VestigeResourceLocator[] cache;

    private JPMSInRepositoryModuleLayerAccessor moduleLayer;

    private boolean jpmsActivated;

    private boolean verified;

    private List<VestigeJar> vestigeJars;

    public AttachedVestigeClassLoader(final List<AttachedVestigeClassLoader> dependencies) {
        this.dependencies = dependencies;
    }

    public AttachedVestigeClassLoader(final VestigeClassLoader<AttachedVestigeClassLoader> vestigeClassLoader, final List<AttachedVestigeClassLoader> dependencies,
            final String name, final boolean attachmentScoped, final VestigeResourceLocator[] cache, final JPMSInRepositoryModuleLayerAccessor moduleLayer,
            final boolean jpmsActivated, final List<VestigeJar> vestigeJars, final boolean verified) {
        this.vestigeClassLoader = vestigeClassLoader;
        this.dependencies = dependencies;
        this.name = name;
        this.attachmentScoped = attachmentScoped;
        softReferences = new ArrayList<SoftReference<?>>();
        this.cache = cache;
        this.moduleLayer = moduleLayer;
        this.jpmsActivated = jpmsActivated;
        this.vestigeJars = vestigeJars;
        this.verified = verified;
    }

    public void setModuleLayer(final JPMSInRepositoryModuleLayerAccessor moduleLayer) {
        this.moduleLayer = moduleLayer;
    }

    public JPMSInRepositoryModuleLayerAccessor getModuleLayer() {
        return moduleLayer;
    }

    public VestigeClassLoader<AttachedVestigeClassLoader> getVestigeClassLoader() {
        return vestigeClassLoader;
    }

    public List<AttachedVestigeClassLoader> getDependencies() {
        return dependencies;
    }

    public String getName() {
        return name;
    }

    public int getAttachments() {
        return attachments;
    }

    public void setAttachments(final int attachments) {
        this.attachments = attachments;
    }

    public List<SoftReference<?>> getSoftReferences() {
        return softReferences;
    }

    public VestigeResourceLocator[] getCache() {
        return cache;
    }

    public boolean isAttachmentScoped() {
        return attachmentScoped;
    }

    public boolean isJPMSActivated() {
        return jpmsActivated;
    }

    public boolean isVerified() {
        return verified;
    }

    public List<VestigeJar> getVestigeJars() {
        List<VestigeJar> vestigeJars = new ArrayList<VestigeJar>();
        vestigeJars.addAll(this.vestigeJars);
        for (AttachedVestigeClassLoader attachedVestigeClassLoader : dependencies) {
            vestigeJars.addAll(attachedVestigeClassLoader.getVestigeJars());
        }
        return vestigeJars;
    }

    @Override
    public String toString() {
        if (name != null) {
            if (jpmsActivated || verified) {
                StringBuilder sb = new StringBuilder();
                sb.append(name);
                sb.append(" (");
                boolean first = true;
                if (jpmsActivated) {
                    sb.append("JPMS");
                    first = false;
                }
                if (verified) {
                    if (first) {
                        first = false;
                    } else {
                        sb.append(",");
                    }
                    sb.append("verified");
                }
                sb.append(")");
                return sb.toString();
            }
            return name;
        }
        return super.toString();
    }

}
