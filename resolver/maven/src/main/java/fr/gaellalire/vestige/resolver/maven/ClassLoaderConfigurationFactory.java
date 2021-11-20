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

package fr.gaellalire.vestige.resolver.maven;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.gaellalire.vestige.core.VestigeClassLoader;
import fr.gaellalire.vestige.core.parser.StringParser;
import fr.gaellalire.vestige.jpms.ModuleDescriptor;
import fr.gaellalire.vestige.jpms.NamedModuleUtils;
import fr.gaellalire.vestige.platform.AddAccessibility;
import fr.gaellalire.vestige.platform.AddReads;
import fr.gaellalire.vestige.platform.ClassLoaderConfiguration;
import fr.gaellalire.vestige.platform.FileWithMetadata;
import fr.gaellalire.vestige.platform.JPMSNamedModulesConfiguration;
import fr.gaellalire.vestige.platform.PatchFileWithMetadata;
import fr.gaellalire.vestige.platform.StringParserFactory;
import fr.gaellalire.vestige.spi.job.TaskHelper;
import fr.gaellalire.vestige.spi.resolver.ResolverException;
import fr.gaellalire.vestige.spi.resolver.Scope;

/**
 * @author Gael Lalire
 */
public class ClassLoaderConfigurationFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClassLoaderConfigurationFactory.class);

    public static final String CLASS_EXTENSION = ".class";

    public static final List<Integer> LOCAL_CLASSLOADER_PATH = Collections.singletonList(-1);

    private MavenClassLoaderConfigurationKey classLoaderConfigurationKey;

    private String appName;

    private List<FileWithMetadata> beforeUrls;

    private List<FileWithMetadata> afterUrls;

    private List<Integer> paths;

    private List<List<Integer>> pathIdsList;

    private List<ClassLoaderConfigurationFactory> factoryDependencies;

    private TreeMap<String, Integer> pathsByResourceName;

    private TreeMap<String, Integer> pathsByClassName;

    private TreeMap<String, Integer> exportedPathsByResourceName;

    private TreeMap<String, Integer> exportedPathsByClassName;

    private Set<String> exportedLocalClassName;

    private Set<String> exportedLocalResourceName;

    private Scope scope;

    private JPMSNamedModulesConfiguration namedModulesConfiguration;

    private List<String> moduleNames;

    private Set<String> dependenciesModuleNames;

    private List<ClassLoaderConfigurationFactory> recursiveFactoryDependencies;

    public List<List<Integer>> getPathIdsList() {
        return pathIdsList;
    }

    public TreeMap<String, Integer> getPathsByResourceName() {
        return pathsByResourceName;
    }

    public TreeMap<String, Integer> getPathsByClassName() {
        return pathsByClassName;
    }

    public MavenClassLoaderConfigurationKey getKey() {
        return classLoaderConfigurationKey;
    }

    public List<FileWithMetadata> getBeforeUrls() {
        return beforeUrls;
    }

    public List<FileWithMetadata> getAfterUrls() {
        return afterUrls;
    }

    public Scope getScope() {
        return scope;
    }

    public List<String> getModuleNames() {
        return moduleNames;
    }

    public static void readJar(final Map<String, List<Integer>> pathsByResourceName, final Map<String, List<Integer>> exportedPathsByResourceName,
            final Set<String> exportedLocalResourceName, final Map<String, List<Integer>> exportedClasses, final Set<String> exportedLocalClassName, final List<String> moduleNames,
            final File url) throws IOException {
        JarInputStream openStream = new JarInputStream(new FileInputStream(url));
        List<String> resources = new ArrayList<String>();
        Set<String> packages = null;
        try {
            String moduleName = null;
            ZipEntry nextEntry = openStream.getNextEntry();
            while (nextEntry != null) {
                String name = nextEntry.getName();
                if (exportedPathsByResourceName != null && NamedModuleUtils.MODULE_INFO_ENTRY_NAME.equals(name)) {
                    ModuleDescriptor descriptor = NamedModuleUtils.getDescriptor(openStream);
                    moduleName = descriptor.name();
                    // seems that nothing is exported (even unqualified open or exported package) maybe automatic ?
                    packages = Collections.emptySet();
                    for (String resource : resources) {
                        if (packages.contains(VestigeClassLoader.getPackageNameFromResourceName(resource))) {
                            exportedPathsByResourceName.put(resource, LOCAL_CLASSLOADER_PATH);
                            exportedLocalResourceName.add(resource);
                        }
                    }
                    resources = null;
                }
                pathsByResourceName.put(name, LOCAL_CLASSLOADER_PATH);
                if (exportedPathsByResourceName != null) {
                    if (name.endsWith(CLASS_EXTENSION)) {
                        // .class are not encapsulated so there are not exported either
                        String className = name.substring(0, name.length() - CLASS_EXTENSION.length()).replace('/', '.');
                        exportedClasses.put(className, LOCAL_CLASSLOADER_PATH);
                        exportedLocalClassName.add(className);
                    } else if (moduleName != null) {
                        if (packages.contains(VestigeClassLoader.getPackageNameFromResourceName(name))) {
                            // ignore
                            exportedPathsByResourceName.put(name, LOCAL_CLASSLOADER_PATH);
                            exportedLocalResourceName.add(name);
                        }
                    } else {
                        resources.add(name);
                    }
                } else {
                    exportedLocalResourceName.add(name);
                }
                nextEntry = openStream.getNextEntry();
            }
            if (exportedPathsByResourceName != null) {
                if (moduleName == null) {
                    Manifest manifest = openStream.getManifest();
                    moduleName = NamedModuleUtils.getAutomaticModuleName(manifest);
                    if (moduleName == null) {
                        moduleName = NamedModuleUtils.getAutomaticModuleName(url.getName());
                    }
                    for (String resource : resources) {
                        exportedPathsByResourceName.put(resource, LOCAL_CLASSLOADER_PATH);
                        exportedLocalResourceName.add(resource);
                    }
                }
                moduleNames.add(moduleName);
            }
        } finally {
            openStream.close();
        }
    }

    public Set<String> getDependenciesModuleNames() {
        return dependenciesModuleNames;
    }

    public List<ClassLoaderConfigurationFactory> getRecursiveFactoryDependencies() {
        return recursiveFactoryDependencies;
    }

    public ClassLoaderConfigurationFactory(final String appName, final MavenClassLoaderConfigurationKey classLoaderConfigurationKey, final Scope scope,
            final List<FileWithMetadata> beforeUrls, final List<FileWithMetadata> afterUrls, final List<ClassLoaderConfigurationFactory> dependencies,
            final boolean encapsulationActivated) throws ResolverException {

        TreeMap<String, List<Integer>> pathsByResourceName = new TreeMap<String, List<Integer>>();
        TreeMap<String, List<Integer>> exportedPathsByResourceName = null;
        TreeMap<String, List<Integer>> exportedPathsByClassName = null;
        if (encapsulationActivated) {
            exportedPathsByResourceName = new TreeMap<String, List<Integer>>();
            exportedPathsByClassName = new TreeMap<String, List<Integer>>();
            moduleNames = new ArrayList<String>();
            this.dependenciesModuleNames = new HashSet<String>();
        }

        this.appName = appName;
        this.beforeUrls = beforeUrls;
        this.afterUrls = afterUrls;
        this.classLoaderConfigurationKey = classLoaderConfigurationKey;
        this.scope = scope;
        this.factoryDependencies = dependencies;
        exportedLocalResourceName = new HashSet<String>();
        exportedLocalClassName = new HashSet<String>();
        paths = new ArrayList<Integer>();

        try {
            for (FileWithMetadata previous : beforeUrls) {
                readJar(pathsByResourceName, exportedPathsByResourceName, exportedLocalResourceName, exportedPathsByClassName, exportedLocalClassName, moduleNames,
                        previous.getFile());
                PatchFileWithMetadata patch = previous.getPatch();
                if (patch != null) {
                    readJar(pathsByResourceName, exportedPathsByResourceName, exportedLocalResourceName, exportedPathsByClassName, exportedLocalClassName, moduleNames,
                            patch.getFile());
                }
            }
        } catch (IOException e) {
            throw new ResolverException("Unable to read jar content", e);
        }

        try {
            for (FileWithMetadata previous : afterUrls) {
                readJar(pathsByResourceName, exportedPathsByResourceName, exportedLocalResourceName, exportedPathsByClassName, exportedLocalClassName, moduleNames,
                        previous.getFile());
                PatchFileWithMetadata patch = previous.getPatch();
                if (patch != null) {
                    readJar(pathsByResourceName, exportedPathsByResourceName, exportedLocalResourceName, exportedPathsByClassName, exportedLocalClassName, moduleNames,
                            patch.getFile());
                }
            }
        } catch (IOException e) {
            throw new ResolverException("Unable to read jar content", e);
        }

        LinkedHashSet<ClassLoaderConfigurationFactory> recursiveFactoryDependenciesSet = new LinkedHashSet<ClassLoaderConfigurationFactory>();

        int pos = 0;
        for (ClassLoaderConfigurationFactory dependency : dependencies) {
            if (recursiveFactoryDependenciesSet.add(dependency)) {
                paths.add(pos);
                paths.add(-1);
            }
            int subPos = 0;
            for (ClassLoaderConfigurationFactory subClassLoaderConfigurationFactory : dependency.getRecursiveFactoryDependencies()) {
                if (recursiveFactoryDependenciesSet.add(subClassLoaderConfigurationFactory)) {
                    paths.add(pos);
                    paths.add(subPos);
                }
                subPos++;
            }
            if (encapsulationActivated) {
                dependenciesModuleNames.addAll(dependency.getModuleNames());
                dependenciesModuleNames.addAll(dependency.getDependenciesModuleNames());
            }
            pos++;
        }

        recursiveFactoryDependencies = new ArrayList<ClassLoaderConfigurationFactory>(recursiveFactoryDependenciesSet);

        pos = 0;
        for (ClassLoaderConfigurationFactory dependency : recursiveFactoryDependencies) {
            if (encapsulationActivated) {
                // could improve by getting only exported and local (getExportedLocalClassName)
                for (String className : dependency.exportedLocalClassName) {
                    List<Integer> list = exportedPathsByClassName.get(className);
                    if (list == null) {
                        list = new ArrayList<Integer>();
                        exportedPathsByClassName.put(className, list);
                    } else if (list == LOCAL_CLASSLOADER_PATH) {
                        list = new ArrayList<Integer>(list);
                        exportedPathsByClassName.put(className, list);
                    }
                    list.add(pos);
                }
                for (String resourceName : dependency.exportedLocalResourceName) {
                    List<Integer> list = exportedPathsByResourceName.get(resourceName);
                    if (list == null) {
                        list = new ArrayList<Integer>();
                        pathsByResourceName.put(resourceName, list);
                        exportedPathsByResourceName.put(resourceName, list);
                    } else if (list == LOCAL_CLASSLOADER_PATH) {
                        list = new ArrayList<Integer>(list);
                        pathsByResourceName.put(resourceName, list);
                        exportedPathsByResourceName.put(resourceName, list);
                    }
                    list.add(pos);
                }
            } else {
                for (String resourceName : dependency.getPathsByResourceName().keySet()) {
                    List<Integer> list = pathsByResourceName.get(resourceName);
                    if (list == null) {
                        list = new ArrayList<Integer>();
                        pathsByResourceName.put(resourceName, list);
                    } else if (list == LOCAL_CLASSLOADER_PATH) {
                        list = new ArrayList<Integer>(list);
                        pathsByResourceName.put(resourceName, list);
                    }
                    list.add(pos);
                }
            }
            pos++;
        }

        HashSet<List<Integer>> set = new HashSet<List<Integer>>(pathsByResourceName.values());
        if (encapsulationActivated) {
            // exportedPathsByResourceName is a subset of pathsByResourceName, no need to add it
            set.addAll(exportedPathsByClassName.values());
        }
        pathIdsList = new ArrayList<List<Integer>>(set.size() + 1);
        // index 0 is parent only
        pathIdsList.add(Collections.<Integer> emptyList());
        pathIdsList.addAll(set);
        this.pathsByResourceName = new TreeMap<String, Integer>();
        for (Entry<String, List<Integer>> entry : pathsByResourceName.entrySet()) {
            String key = entry.getKey();
            this.pathsByResourceName.put(key, pathIdsList.indexOf(entry.getValue()));
        }
        if (encapsulationActivated) {
            this.exportedPathsByResourceName = new TreeMap<String, Integer>();
            for (Entry<String, List<Integer>> entry : exportedPathsByResourceName.entrySet()) {
                String key = entry.getKey();
                this.exportedPathsByResourceName.put(key, pathIdsList.indexOf(entry.getValue()));
            }
            this.exportedPathsByClassName = new TreeMap<String, Integer>();
            for (Entry<String, List<Integer>> entry : exportedPathsByClassName.entrySet()) {
                String key = entry.getKey();
                this.exportedPathsByClassName.put(key, pathIdsList.indexOf(entry.getValue()));
            }
        }

    }

    private ClassLoaderConfiguration cachedClassLoaderConfiguration;

    public void setNamedModulesConfiguration(final JPMSNamedModulesConfiguration namedModulesConfiguration) {
        JPMSNamedModulesConfiguration subNamedModulesConfiguration;
        Set<AddAccessibility> addExports = namedModulesConfiguration.getAddExports();
        Set<AddAccessibility> addOpens = namedModulesConfiguration.getAddOpens();
        Set<AddReads> addReads = namedModulesConfiguration.getAddReads();
        if (JPMSNamedModulesConfiguration.EMPTY_INSTANCE.equals(namedModulesConfiguration)) {
            this.namedModulesConfiguration = JPMSNamedModulesConfiguration.EMPTY_INSTANCE;
            subNamedModulesConfiguration = JPMSNamedModulesConfiguration.EMPTY_INSTANCE;
        } else {
            Set<AddAccessibility> subAddExports = null;
            Set<AddAccessibility> currentAddExports = null;
            if (addExports != null) {
                for (AddAccessibility addAccessibility : addExports) {
                    if (dependenciesModuleNames.contains(addAccessibility.getSource()) && dependenciesModuleNames.contains(addAccessibility.getTarget())) {
                        // valid one
                        if (currentAddExports == null) {
                            currentAddExports = new HashSet<AddAccessibility>();
                        }
                        currentAddExports.add(addAccessibility);

                        if (!moduleNames.contains(addAccessibility.getSource()) && !moduleNames.contains(addAccessibility.getTarget())) {
                            // not for us (but our dependency will used it)
                            if (subAddExports == null) {
                                subAddExports = new HashSet<AddAccessibility>();
                            }
                            subAddExports.add(addAccessibility);
                        }
                    }
                }
            }
            Set<AddAccessibility> subAddOpens = null;
            Set<AddAccessibility> currentAddOpens = null;
            if (addOpens != null) {
                for (AddAccessibility addAccessibility : addOpens) {

                    if (dependenciesModuleNames.contains(addAccessibility.getSource()) && dependenciesModuleNames.contains(addAccessibility.getTarget())) {
                        // valid one
                        if (currentAddOpens == null) {
                            currentAddOpens = new HashSet<AddAccessibility>();
                        }
                        currentAddOpens.add(addAccessibility);

                        if (!moduleNames.contains(addAccessibility.getSource()) && !moduleNames.contains(addAccessibility.getTarget())) {
                            // not for us (but our dependency will used it)
                            if (subAddOpens == null) {
                                subAddOpens = new HashSet<AddAccessibility>();
                            }
                            subAddOpens.add(addAccessibility);
                        }
                    }
                }
            }

            Set<AddReads> subAddReads = addReads;
            Set<AddReads> currentAddReads = null;
            if (addReads != null) {
                for (AddReads addAccessibility : addReads) {

                    if (dependenciesModuleNames.contains(addAccessibility.getSource()) && dependenciesModuleNames.contains(addAccessibility.getTarget())) {
                        // valid one
                        if (currentAddReads == null) {
                            currentAddReads = new HashSet<AddReads>();
                        }
                        currentAddReads.add(addAccessibility);

                        if (!moduleNames.contains(addAccessibility.getSource()) && !moduleNames.contains(addAccessibility.getTarget())) {
                            // not for us (but our dependency will used it)
                            if (subAddReads == null) {
                                subAddReads = new HashSet<AddReads>();
                            }
                            subAddReads.add(addAccessibility);
                        }
                    }
                }
            }

            if (currentAddReads == null && currentAddExports == null && currentAddOpens == null) {
                this.namedModulesConfiguration = JPMSNamedModulesConfiguration.EMPTY_INSTANCE;
            } else {
                this.namedModulesConfiguration = new JPMSNamedModulesConfiguration(currentAddReads, currentAddExports, currentAddOpens);
            }
            if (subAddReads == null && subAddExports == null && subAddOpens == null) {
                subNamedModulesConfiguration = JPMSNamedModulesConfiguration.EMPTY_INSTANCE;
            } else {
                subNamedModulesConfiguration = new JPMSNamedModulesConfiguration(subAddReads, subAddExports, subAddOpens);
            }
        }
        for (ClassLoaderConfigurationFactory dep : factoryDependencies) {
            dep.setNamedModulesConfiguration(subNamedModulesConfiguration);
        }
    }

    public ClassLoaderConfiguration create(final StringParserFactory stringParserFactory, final TaskHelper taskHelper, final int totalDependencies, final float currentProgress,
            final float taskProgress, final int[] treated) {
        if (cachedClassLoaderConfiguration == null) {
            List<MavenClassLoaderConfigurationKey> keyDependencies = new ArrayList<MavenClassLoaderConfigurationKey>(factoryDependencies.size());
            List<ClassLoaderConfiguration> dependencies = new ArrayList<ClassLoaderConfiguration>(factoryDependencies.size());
            for (ClassLoaderConfigurationFactory classLoaderConfigurationFactory : factoryDependencies) {
                dependencies.add(classLoaderConfigurationFactory.create(stringParserFactory, taskHelper, totalDependencies, currentProgress, taskProgress, treated));
                scope = scope.restrict(classLoaderConfigurationFactory.getScope());
                keyDependencies.add(classLoaderConfigurationFactory.getKey());
            }
            LOGGER.trace("Creating classloader rules for {}", classLoaderConfigurationKey.getArtifacts());
            StringParser pathsByResourceName = stringParserFactory.createStringParser(this.pathsByResourceName, 0);
            StringParser pathsByClassName = null;
            if (this.exportedPathsByClassName != null) {
                pathsByClassName = stringParserFactory.createStringParser(this.exportedPathsByClassName, 0);
            }
            String name;
            if (scope == Scope.PLATFORM) {
                name = classLoaderConfigurationKey.getArtifacts().toString();
            } else {
                classLoaderConfigurationKey = new MavenClassLoaderConfigurationKey(classLoaderConfigurationKey.getArtifacts(), keyDependencies, scope,
                        classLoaderConfigurationKey.getModuleConfiguration(), namedModulesConfiguration, classLoaderConfigurationKey.getBeforeParents());
                name = classLoaderConfigurationKey.getArtifacts().toString() + " of " + appName;
            }
            boolean mdcIncluded = false;
            for (DefaultMavenArtifact mavenArtifact : classLoaderConfigurationKey.getArtifacts()) {
                if ("org.slf4j".equals(mavenArtifact.getGroupId()) && "slf4j-api".equals(mavenArtifact.getArtifactId())) {
                    mdcIncluded = true;
                }
            }
            cachedClassLoaderConfiguration = new ClassLoaderConfiguration(classLoaderConfigurationKey, name, scope == Scope.ATTACHMENT, beforeUrls, afterUrls, dependencies, paths,
                    pathIdsList, pathsByResourceName, pathsByClassName, classLoaderConfigurationKey.getModuleConfiguration(), namedModulesConfiguration, mdcIncluded);
            LOGGER.trace("Classloader rules created");
            treated[0]++;
            taskHelper.setProgress(currentProgress + taskProgress * (((float) treated[0]) / totalDependencies));
        }
        return cachedClassLoaderConfiguration;
    }

    @Override
    public String toString() {
        return classLoaderConfigurationKey.toString();
    }

    public boolean removeFile(final File fileExcluded) {
        Iterator<FileWithMetadata> iterator = afterUrls.iterator();
        while (iterator.hasNext()) {
            if (fileExcluded.equals(iterator.next().getFile())) {
                iterator.remove();
                return true;
            }
        }
        iterator = beforeUrls.iterator();
        while (iterator.hasNext()) {
            if (fileExcluded.equals(iterator.next().getFile())) {
                iterator.remove();
                return true;
            }
        }
        return false;
    }

}
