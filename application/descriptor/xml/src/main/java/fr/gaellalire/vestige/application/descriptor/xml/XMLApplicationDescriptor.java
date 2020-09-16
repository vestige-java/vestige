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

package fr.gaellalire.vestige.application.descriptor.xml;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.Permission;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fr.gaellalire.vestige.application.descriptor.xml.schema.application.ActivateNamedModules;
import fr.gaellalire.vestige.application.descriptor.xml.schema.application.AddExports;
import fr.gaellalire.vestige.application.descriptor.xml.schema.application.AddOpens;
import fr.gaellalire.vestige.application.descriptor.xml.schema.application.AddReads;
import fr.gaellalire.vestige.application.descriptor.xml.schema.application.Application;
import fr.gaellalire.vestige.application.descriptor.xml.schema.application.Inject;
import fr.gaellalire.vestige.application.descriptor.xml.schema.application.Installer;
import fr.gaellalire.vestige.application.descriptor.xml.schema.application.Launcher;
import fr.gaellalire.vestige.application.descriptor.xml.schema.application.MavenClassType;
import fr.gaellalire.vestige.application.descriptor.xml.schema.application.Mode;
import fr.gaellalire.vestige.application.descriptor.xml.schema.application.ModifyLoadedDependency;
import fr.gaellalire.vestige.application.descriptor.xml.schema.application.ModifyScope;
import fr.gaellalire.vestige.application.descriptor.xml.schema.application.ModulePackageName;
import fr.gaellalire.vestige.application.descriptor.xml.schema.application.URLsClassType;
import fr.gaellalire.vestige.application.manager.AddInject;
import fr.gaellalire.vestige.application.manager.ApplicationDescriptor;
import fr.gaellalire.vestige.application.manager.ApplicationException;
import fr.gaellalire.vestige.application.manager.ApplicationResolvedClassLoaderConfiguration;
import fr.gaellalire.vestige.application.manager.VersionUtils;
import fr.gaellalire.vestige.spi.job.JobHelper;
import fr.gaellalire.vestige.spi.resolver.ResolverException;
import fr.gaellalire.vestige.spi.resolver.Scope;
import fr.gaellalire.vestige.spi.resolver.maven.CreateClassLoaderConfigurationRequest;
import fr.gaellalire.vestige.spi.resolver.maven.MavenContext;
import fr.gaellalire.vestige.spi.resolver.maven.ModifyLoadedDependencyRequest;
import fr.gaellalire.vestige.spi.resolver.maven.ResolveMavenArtifactRequest;
import fr.gaellalire.vestige.spi.resolver.maven.ResolveMode;
import fr.gaellalire.vestige.spi.resolver.maven.ResolvedMavenArtifact;
import fr.gaellalire.vestige.spi.resolver.url_list.URLListRequest;
import fr.gaellalire.vestige.utils.AnyURIProperty;
import fr.gaellalire.vestige.utils.SimpleValueGetter;

/**
 * @author Gael Lalire
 */
public class XMLApplicationDescriptor implements ApplicationDescriptor {

    private static final Pattern VERSION_RANGE_PATTERN = Pattern.compile("(\\d+)\\.(\\d+)\\.(\\d+)-(\\d+)");

    private XMLApplicationRepositoryManager xmlApplicationRepositoryManager;

    private List<Integer> version;

    private Application application;

    private MavenContext mavenContext;

    private Set<Permission> installerPermissions;

    private Set<Permission> permissions;

    private JobHelper jobHelper;

    private String javaSpecificationVersion;

    public XMLApplicationDescriptor(final XMLApplicationRepositoryManager xmlApplicationRepositoryManager, final String javaSpecificationVersion, final List<Integer> version,
            final Application application, final MavenContext mavenConfigResolved, final Set<Permission> permissions, final Set<Permission> installerPermissions,
            final JobHelper jobHelper) {
        this.xmlApplicationRepositoryManager = xmlApplicationRepositoryManager;
        this.javaSpecificationVersion = javaSpecificationVersion;
        this.version = version;
        this.application = application;
        this.mavenContext = mavenConfigResolved;
        this.permissions = permissions;
        this.installerPermissions = installerPermissions;
        this.jobHelper = jobHelper;
    }

    @Override
    public String getJavaSpecificationVersion() throws ApplicationException {
        return javaSpecificationVersion;
    }

    public Set<List<Integer>> getSupportedMigrationVersions() throws ApplicationException {
        Installer installer = application.getInstaller();
        if (installer == null) {
            return Collections.emptySet();
        }
        Set<List<Integer>> supportedMigrationVersion = new HashSet<List<Integer>>();
        List<String> supportedMigration = installer.getSupportedMigration();
        if (supportedMigration != null) {
            for (String string : supportedMigration) {
                Matcher matcher = VERSION_RANGE_PATTERN.matcher(string);
                if (!matcher.matches()) {
                    throw new ApplicationException("illegal range");
                }
                int g = 1;
                int major = Integer.parseInt(matcher.group(g++));
                int minor = Integer.parseInt(matcher.group(g++));
                int firstBugfix = Integer.parseInt(matcher.group(g++));
                int lastBugfix = Integer.parseInt(matcher.group(g++));
                for (int i = firstBugfix; i <= lastBugfix; i++) {
                    List<Integer> otherVersion = Arrays.asList(major, minor, i);
                    Integer compare = VersionUtils.compare(version, otherVersion);
                    if (compare != null && compare.intValue() < 0) {
                        throw new ApplicationException("Version " + VersionUtils.toString(otherVersion) + " cannot be supported by " + VersionUtils.toString(version));
                    }
                    supportedMigrationVersion.add(otherVersion);
                }
            }
        }
        return supportedMigrationVersion;
    }

    public Set<List<Integer>> getUninterruptedMigrationVersions() throws ApplicationException {
        Installer installer = application.getInstaller();
        if (installer == null) {
            return Collections.emptySet();
        }
        Set<List<Integer>> uninterruptedMigrationVersion = new HashSet<List<Integer>>();
        List<String> uninterruptedMigration = installer.getUninterruptedMigration();
        if (uninterruptedMigration != null) {
            for (String string : uninterruptedMigration) {
                Matcher matcher = VERSION_RANGE_PATTERN.matcher(string);
                if (!matcher.matches()) {
                    throw new ApplicationException("illegal range");
                }
                int g = 1;
                int major = Integer.parseInt(matcher.group(g++));
                int minor = Integer.parseInt(matcher.group(g++));
                int firstBugfix = Integer.parseInt(matcher.group(g++));
                int lastBugfix = Integer.parseInt(matcher.group(g++));
                for (int i = firstBugfix; i <= lastBugfix; i++) {
                    List<Integer> otherVersion = Arrays.asList(major, minor, i);
                    Integer compare = VersionUtils.compare(version, otherVersion);
                    if (compare != null && compare.intValue() < 0) {
                        throw new ApplicationException("Version " + VersionUtils.toString(otherVersion) + " cannot be supported by " + VersionUtils.toString(version));
                    }
                    uninterruptedMigrationVersion.add(otherVersion);
                }
            }
        }
        return uninterruptedMigrationVersion;
    }

    public String getInstallerClassName() throws ApplicationException {
        Installer installer = application.getInstaller();
        if (installer == null) {
            return null;
        }
        return SimpleValueGetter.INSTANCE.getValue(installer.getClazz());
    }

    public ApplicationResolvedClassLoaderConfiguration getInstallerClassLoaderConfiguration(final String configurationName) throws ApplicationException {
        Installer installer = application.getInstaller();
        if (installer == null) {
            return null;
        }
        URLsClassType urlsInstaller = installer.getUrlListResolver();
        if (urlsInstaller != null) {
            URLListRequest urlListRequest = xmlApplicationRepositoryManager.getVestigeURLListResolver().createURLListRequest(convertScope(urlsInstaller.getScope()),
                    configurationName);

            for (AnyURIProperty string : urlsInstaller.getUrl()) {
                try {
                    urlListRequest.addURL(new URL(SimpleValueGetter.INSTANCE.getValue(string)));
                } catch (MalformedURLException e) {
                    throw new ApplicationException("Not an URL", e);
                }
            }

            for (fr.gaellalire.vestige.application.descriptor.xml.schema.application.ModulePackageName modulePackageName : urlsInstaller.getAddExports()) {
                urlListRequest.addExports(SimpleValueGetter.INSTANCE.getValue(modulePackageName.getModule()), SimpleValueGetter.INSTANCE.getValue(modulePackageName.getPackage()));
            }
            for (fr.gaellalire.vestige.application.descriptor.xml.schema.application.ModulePackageName modulePackageName : urlsInstaller.getAddOpens()) {
                urlListRequest.addOpens(SimpleValueGetter.INSTANCE.getValue(modulePackageName.getModule()), SimpleValueGetter.INSTANCE.getValue(modulePackageName.getPackage()));
            }

            try {
                return new ApplicationResolvedClassLoaderConfiguration(urlListRequest.execute(jobHelper), xmlApplicationRepositoryManager.getVestigeURLListResolverIndex());
            } catch (ResolverException e) {
                throw new ApplicationException(e);
            }
        }
        return resolve(configurationName, installer.getMavenResolver(), jobHelper);
    }

    public String getLauncherClassName() throws ApplicationException {
        Launcher launcher = application.getLauncher();
        return SimpleValueGetter.INSTANCE.getValue(launcher.getClazz());
    }

    public boolean isInstallerPrivateSystem() throws ApplicationException {
        Installer installer = application.getInstaller();
        if (installer == null) {
            return false;
        }
        return SimpleValueGetter.INSTANCE.getValue(installer.getPrivateSystem());
    }

    public boolean isLauncherPrivateSystem() throws ApplicationException {
        Launcher launcher = application.getLauncher();
        if (launcher == null) {
            return false;
        }
        return SimpleValueGetter.INSTANCE.getValue(launcher.getPrivateSystem());
    }

    public List<AddInject> getLauncherAddInjects() {
        List<AddInject> addInjects = new ArrayList<AddInject>();
        for (Inject inject : application.getLauncher().getInject()) {
            addInjects.add(new AddInject(SimpleValueGetter.INSTANCE.getValue(inject.getServiceClassName()), SimpleValueGetter.INSTANCE.getValue(inject.getTargetServiceClassName()),
                    SimpleValueGetter.INSTANCE.getValue(inject.getSetterName())));
        }
        return addInjects;
    }

    @Override
    public List<AddInject> getInstallerAddInjects() throws ApplicationException {
        Installer installer = application.getInstaller();
        if (installer == null) {
            return Collections.<AddInject> emptyList();
        }
        List<AddInject> addInjects = new ArrayList<AddInject>();
        for (Inject inject : installer.getInject()) {
            addInjects.add(new AddInject(SimpleValueGetter.INSTANCE.getValue(inject.getServiceClassName()), SimpleValueGetter.INSTANCE.getValue(inject.getTargetServiceClassName()),
                    SimpleValueGetter.INSTANCE.getValue(inject.getSetterName())));
        }
        return addInjects;
    }

    public ApplicationResolvedClassLoaderConfiguration getLauncherClassLoaderConfiguration(final String configurationName) throws ApplicationException {
        Launcher launcher = application.getLauncher();
        URLsClassType urlsLauncher = launcher.getUrlListResolver();
        if (urlsLauncher != null) {
            URLListRequest urlListRequest = xmlApplicationRepositoryManager.getVestigeURLListResolver().createURLListRequest(convertScope(urlsLauncher.getScope()),
                    configurationName);

            for (AnyURIProperty string : urlsLauncher.getUrl()) {
                try {
                    urlListRequest.addURL(new URL(SimpleValueGetter.INSTANCE.getValue(string)));
                } catch (MalformedURLException e) {
                    throw new ApplicationException("Not an URL", e);
                }
            }

            for (fr.gaellalire.vestige.application.descriptor.xml.schema.application.ModulePackageName modulePackageName : urlsLauncher.getAddExports()) {
                urlListRequest.addExports(SimpleValueGetter.INSTANCE.getValue(modulePackageName.getModule()), SimpleValueGetter.INSTANCE.getValue(modulePackageName.getPackage()));
            }
            for (fr.gaellalire.vestige.application.descriptor.xml.schema.application.ModulePackageName modulePackageName : urlsLauncher.getAddOpens()) {
                urlListRequest.addOpens(SimpleValueGetter.INSTANCE.getValue(modulePackageName.getModule()), SimpleValueGetter.INSTANCE.getValue(modulePackageName.getPackage()));
            }
            urlsLauncher.getScope();

            try {
                return new ApplicationResolvedClassLoaderConfiguration(urlListRequest.execute(jobHelper), xmlApplicationRepositoryManager.getVestigeURLListResolverIndex());
            } catch (ResolverException e) {
                throw new ApplicationException(e);
            }
        }
        return resolve(configurationName, launcher.getMavenResolver(), jobHelper);
    }

    public static Scope convertScope(final fr.gaellalire.vestige.application.descriptor.xml.schema.application.Scope scope) throws ApplicationException {
        Scope mavenScope;
        switch (scope) {
        case ATTACHMENT:
            mavenScope = Scope.ATTACHMENT;
            break;
        case INSTALLATION:
            mavenScope = Scope.CLASS_LOADER_CONFIGURATION;
            break;
        case PLATFORM:
            mavenScope = Scope.PLATFORM;
            break;
        default:
            throw new ApplicationException("Unknown scope " + scope);
        }
        return mavenScope;
    }

    public ApplicationResolvedClassLoaderConfiguration resolve(final String configurationName, final MavenClassType mavenClassType, final JobHelper actionHelper)
            throws ApplicationException {
        ResolveMode resolveMode;
        Mode mode = mavenClassType.getMode();
        switch (mode) {
        case CLASSPATH:
            resolveMode = ResolveMode.CLASSPATH;
            break;
        case FIXED_DEPENDENCIES:
            resolveMode = ResolveMode.FIXED_DEPENDENCIES;
            break;
        default:
            throw new ApplicationException("Unknown launch mode " + mode);
        }

        ResolveMavenArtifactRequest resolveMavenArtifactRequest = mavenContext.resolve(SimpleValueGetter.INSTANCE.getValue(mavenClassType.getGroupId()),
                SimpleValueGetter.INSTANCE.getValue(mavenClassType.getArtifactId()), SimpleValueGetter.INSTANCE.getValue(mavenClassType.getVersion()));

        ResolvedMavenArtifact resolvedMavenArtifact;
        try {
            resolvedMavenArtifact = resolveMavenArtifactRequest.execute(actionHelper);
        } catch (ResolverException e) {
            throw new ApplicationException(e);
        }

        CreateClassLoaderConfigurationRequest createClassLoaderConfigurationRequest = resolvedMavenArtifact.createClassLoaderConfiguration(configurationName, resolveMode,
                convertScope(mavenClassType.getScope()));

        for (ModifyScope modifyScope : mavenClassType.getModifyScope()) {
            createClassLoaderConfigurationRequest.addModifyScope(SimpleValueGetter.INSTANCE.getValue(modifyScope.getGroupId()),
                    SimpleValueGetter.INSTANCE.getValue(modifyScope.getArtifactId()), convertScope(modifyScope.getScope()));
        }

        List<ModifyLoadedDependency> modifyLoadedDependencyList = mavenClassType.getModifyLoadedDependency();
        if (modifyLoadedDependencyList != null) {
            for (ModifyLoadedDependency modifyDependency : modifyLoadedDependencyList) {
                ModifyLoadedDependencyRequest modifyDependencyRequest = createClassLoaderConfigurationRequest.addModifyLoadedDependency(
                        SimpleValueGetter.INSTANCE.getValue(modifyDependency.getGroupId()), SimpleValueGetter.INSTANCE.getValue(modifyDependency.getArtifactId()));
                for (ModulePackageName addExports : modifyDependency.getAddExports()) {
                    modifyDependencyRequest.addExports(SimpleValueGetter.INSTANCE.getValue(addExports.getModule()), SimpleValueGetter.INSTANCE.getValue(addExports.getPackage()));
                }
                for (ModulePackageName addExports : modifyDependency.getAddOpens()) {
                    modifyDependencyRequest.addOpens(SimpleValueGetter.INSTANCE.getValue(addExports.getModule()), SimpleValueGetter.INSTANCE.getValue(addExports.getPackage()));
                }
                if (modifyDependency.getAddBeforeParent() != null) {
                    modifyDependencyRequest.setBeforeParent(true);
                }
                modifyDependencyRequest.execute();
            }
        }

        ActivateNamedModules activateNamedModules = mavenClassType.getActivateNamedModules();
        if (activateNamedModules != null) {
            createClassLoaderConfigurationRequest.setNamedModuleActivated(true);
            for (AddReads addReads : activateNamedModules.getAddReads()) {
                createClassLoaderConfigurationRequest.addReads(SimpleValueGetter.INSTANCE.getValue(addReads.getSource()),
                        SimpleValueGetter.INSTANCE.getValue(addReads.getTarget()));
            }
            for (AddExports addExports : activateNamedModules.getAddExports()) {
                createClassLoaderConfigurationRequest.addExports(SimpleValueGetter.INSTANCE.getValue(addExports.getSource()),
                        SimpleValueGetter.INSTANCE.getValue(addExports.getPn()), SimpleValueGetter.INSTANCE.getValue(addExports.getTarget()));
            }
            for (AddOpens addOpens : activateNamedModules.getAddOpens()) {
                createClassLoaderConfigurationRequest.addOpens(SimpleValueGetter.INSTANCE.getValue(addOpens.getSource()), SimpleValueGetter.INSTANCE.getValue(addOpens.getPn()),
                        SimpleValueGetter.INSTANCE.getValue(addOpens.getTarget()));
            }
        }

        try {
            return new ApplicationResolvedClassLoaderConfiguration(createClassLoaderConfigurationRequest.execute(), xmlApplicationRepositoryManager.getVestigeMavenResolverIndex());
        } catch (ResolverException e) {
            throw new ApplicationException(e);
        }

    }

    @Override
    public Set<Permission> getInstallerPermissions() throws ApplicationException {
        return installerPermissions;
    }

    @Override
    public Set<Permission> getPermissions() throws ApplicationException {
        return permissions;
    }

}
