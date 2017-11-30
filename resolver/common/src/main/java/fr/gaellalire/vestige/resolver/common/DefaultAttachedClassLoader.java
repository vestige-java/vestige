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

import fr.gaellalire.vestige.platform.VestigePlatform;
import fr.gaellalire.vestige.spi.resolver.AttachableClassLoader;
import fr.gaellalire.vestige.spi.resolver.AttachedClassLoader;

/**
 * @author Gael Lalire
 */
public class DefaultAttachedClassLoader implements AttachedClassLoader {

    private VestigePlatform vestigePlatform;

    private int installerAttach;

    private DefaultAttachableClassLoader attachableClassLoader;

    public DefaultAttachedClassLoader(final VestigePlatform vestigePlatform, final int installerAttach, final DefaultAttachableClassLoader attachableClassLoader) {
        this.vestigePlatform = vestigePlatform;
        this.installerAttach = installerAttach;
        this.attachableClassLoader = attachableClassLoader;
    }

    @Override
    public void detach() {
        synchronized (vestigePlatform) {
            vestigePlatform.detach(installerAttach);
        }
        attachableClassLoader = null;
    }

    @Override
    public AttachableClassLoader getAttachableClassLoader() {
        return attachableClassLoader;
    }
}
