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

package fr.gaellalire.vestige.admin.command;

import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import fr.gaellalire.vestige.admin.command.argument.Argument;
import fr.gaellalire.vestige.platform.AttachedVestigeClassLoader;
import fr.gaellalire.vestige.platform.VestigePlatform;

/**
 * @author Gael Lalire
 */
public class Platform implements Command {

    private VestigePlatform vestigePlatform;

    public Platform(final VestigePlatform vestigePlatform) {
        this.vestigePlatform = vestigePlatform;
    }

    public String getName() {
        return "platform";
    }

    public String getDesc() {
        return "Platform state";
    }

    public List<Argument> getArguments() {
        return Collections.emptyList();
    }

    public void add(final Set<AttachedVestigeClassLoader> set, final AttachedVestigeClassLoader attachedVestigeClassLoader) {
        set.add(attachedVestigeClassLoader);
        for (AttachedVestigeClassLoader depAttachedVestigeClassLoader : attachedVestigeClassLoader.getDependencies()) {
            add(set, depAttachedVestigeClassLoader);
        }
    }

    public void execute(final PrintWriter out) {
        Map<AttachedVestigeClassLoader, Integer> map = new HashMap<AttachedVestigeClassLoader, Integer>();
        Set<Integer> attachments = vestigePlatform.getAttachments();
        for (Integer integer : attachments) {
            Set<AttachedVestigeClassLoader> set = new HashSet<AttachedVestigeClassLoader>();
            add(set, vestigePlatform.getAttachedVestigeClassLoader(integer.intValue()));
            for (AttachedVestigeClassLoader attachedVestigeClassLoader : set) {
                Integer count = map.get(attachedVestigeClassLoader);
                if (count == null) {
                    count = 0;
                }
                map.put(attachedVestigeClassLoader, count + 1);
            }
        }
        Map<Integer, Integer> classloadersByCount = new TreeMap<Integer, Integer>();
        for (Integer count : map.values()) {
            Integer integer = classloadersByCount.get(count);
            if (integer == null) {
                integer = 0;
            }
            classloadersByCount.put(count, integer + 1);
        }
        out.println("Platform has " + vestigePlatform.getClassLoaderKeys().size() + " classloader(s) for " + attachments.size() + " attachment(s)");
        for (Entry<Integer, Integer> entry : classloadersByCount.entrySet()) {
            Integer key = entry.getKey();
            String classLoaders;
            if (entry.getValue().intValue() > 1) {
                classLoaders = entry.getValue() + " classLoaders";
            } else {
                classLoaders = entry.getValue() + " classLoader";
            }
            if (key.intValue() == 1) {
                out.println("  " + classLoaders + " are not shared");
            } else {
                out.println("  " + classLoaders + " are shared by " + key + " attachments");
            }
        }

        List<AttachedVestigeClassLoader> unattached = new ArrayList<AttachedVestigeClassLoader>();
        for (Serializable classLoaderKey : vestigePlatform.getClassLoaderKeys()) {
            AttachedVestigeClassLoader attachedVestigeClassLoaderByKey = vestigePlatform.getAttachedVestigeClassLoaderByKey(classLoaderKey);
            if (attachedVestigeClassLoaderByKey != null && !map.containsKey(attachedVestigeClassLoaderByKey)) {
                unattached.add(attachedVestigeClassLoaderByKey);
            }
        }
        if (unattached.size() != 0) {
            out.println("Platform has " + unattached.size() + " classloader(s) without attachment:");
            for (AttachedVestigeClassLoader attachedVestigeClassLoader : unattached) {
                out.println("  " + attachedVestigeClassLoader);
            }
        }

    }

}
