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

import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Stack;

import fr.gaellalire.vestige.platform.ClassLoaderConfiguration;
import fr.gaellalire.vestige.platform.FileWithMetadata;

/**
 * @author Gael Lalire
 */
public class DefaultVestigeJarContext {

    private IdentityHashMap<ClassLoaderConfiguration, Boolean> map;

    private Iterator<FileWithMetadata> beforeSecureFiles;

    private Iterator<FileWithMetadata> afterSecureFiles;

    private Stack<Iterator<ClassLoaderConfiguration>> dependenciesStack;

    private Iterator<ClassLoaderConfiguration> currentDependencies;

    private boolean firstBeforeParent;

    public DefaultVestigeJarContext(final ClassLoaderConfiguration initialClassLoaderConfiguration, final boolean firstBeforeParent) {
        this.map = new IdentityHashMap<ClassLoaderConfiguration, Boolean>();
        this.beforeSecureFiles = initialClassLoaderConfiguration.getBeforeUrls().iterator();
        this.afterSecureFiles = initialClassLoaderConfiguration.getAfterUrls().iterator();
        map.put(initialClassLoaderConfiguration, Boolean.TRUE);
        dependenciesStack = new Stack<Iterator<ClassLoaderConfiguration>>();
        currentDependencies = initialClassLoaderConfiguration.getDependencies().iterator();
        this.firstBeforeParent = firstBeforeParent;
    }

    private FileWithMetadata calculatedNext;

    public boolean hasNext() {
        if (calculatedNext == null) {
            calculatedNext = calculateNext();
        }
        if (calculatedNext == null) {
            return false;
        }
        return true;
    }

    public FileWithMetadata next() {
        FileWithMetadata result;
        if (hasNext()) {
            result = calculatedNext;
            calculatedNext = null;
            return result;
        }
        throw new NoSuchElementException();
    }

    public FileWithMetadata calculateNext() {
        FileWithMetadata nextSecureFile = null;
        if (!firstBeforeParent) {
            // change the first jar
            if (afterSecureFiles.hasNext()) {
                nextSecureFile = afterSecureFiles.next();
            }
            firstBeforeParent = true;
        }
        while (nextSecureFile == null) {
            if (beforeSecureFiles.hasNext()) {
                nextSecureFile = beforeSecureFiles.next();
            } else if (afterSecureFiles.hasNext()) {
                nextSecureFile = afterSecureFiles.next();
            } else {
                if (currentDependencies.hasNext()) {
                    ClassLoaderConfiguration nextClassLoaderConfiguration = currentDependencies.next();
                    if (map.put(nextClassLoaderConfiguration, Boolean.TRUE) == null) {
                        beforeSecureFiles = nextClassLoaderConfiguration.getBeforeUrls().iterator();
                        afterSecureFiles = nextClassLoaderConfiguration.getAfterUrls().iterator();
                        dependenciesStack.push(currentDependencies);
                        currentDependencies = nextClassLoaderConfiguration.getDependencies().iterator();
                    }
                } else {
                    if (dependenciesStack.isEmpty()) {
                        break;
                    }
                    currentDependencies = dependenciesStack.pop();
                }
            }
        }
        return nextSecureFile;
    }

}
