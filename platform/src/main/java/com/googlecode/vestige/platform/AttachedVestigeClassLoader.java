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

package com.googlecode.vestige.platform;

import java.util.ArrayList;
import java.util.List;

import com.googlecode.vestige.core.VestigeClassLoader;

/**
 * @author Gael Lalire
 */
public class AttachedVestigeClassLoader {

    private List<Object> objects;

    private VestigeClassLoader<AttachedVestigeClassLoader> vestigeClassLoader;

    private List<AttachedVestigeClassLoader> dependencies;

    private int attachments;

    private String urls;

    private List<String> startStopClasses;

    private String name;

    public AttachedVestigeClassLoader(final List<AttachedVestigeClassLoader> dependencies) {
        this.dependencies = dependencies;
    }

    public AttachedVestigeClassLoader(final VestigeClassLoader<AttachedVestigeClassLoader> vestigeClassLoader, final List<AttachedVestigeClassLoader> dependencies,
            final String urls, final List<String> startStopClasses, final String name) {
        this.vestigeClassLoader = vestigeClassLoader;
        this.dependencies = dependencies;
        this.urls = urls;
        this.startStopClasses = startStopClasses;
        this.name = name;
        objects = new ArrayList<Object>();
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

    public String getUrls() {
        return urls;
    }

    public List<String> getStartStopClasses() {
        return startStopClasses;
    }

    public void addObject(final Object o) {
        objects.add(o);
    }

    public void removeObject(final Object o) {
        objects.remove(o);
    }

    @Override
    public String toString() {
        if (name != null) {
            return name.toString();
        }
        return super.toString();
    }

}
