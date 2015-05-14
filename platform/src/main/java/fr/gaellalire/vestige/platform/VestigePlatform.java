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

import java.io.Serializable;
import java.util.List;
import java.util.Set;

import fr.gaellalire.vestige.core.VestigeClassLoader;

/**
 * @author Gael Lalire
 */
public interface VestigePlatform {

    int attach(VestigeClassLoader<AttachedVestigeClassLoader> classLoader);

    int attach(ClassLoaderConfiguration classLoaderConfiguration) throws InterruptedException;

    void detach(int id);

    void start(int id);

    void stop(int id);

    boolean isStarted(int id);

    List<Serializable> getClassLoaderKeys();

    AttachedVestigeClassLoader getAttachedVestigeClassLoaderByKey(Serializable key);

    Set<Integer> getAttachments();

    VestigeClassLoader<AttachedVestigeClassLoader> getClassLoader(int id);

    AttachedVestigeClassLoader getAttachedVestigeClassLoader(int id);

}