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

import fr.gaellalire.vestige.application.descriptor.xml.schema.application.Application;
import fr.gaellalire.vestige.application.descriptor.xml.schema.application.Installer;
import fr.gaellalire.vestige.application.descriptor.xml.schema.application.Launcher;
import fr.gaellalire.vestige.application.descriptor.xml.schema.application.MavenClassType;
import fr.gaellalire.vestige.application.descriptor.xml.schema.application.Mode;
import fr.gaellalire.vestige.application.descriptor.xml.schema.application.ModifyScope;
import fr.gaellalire.vestige.application.descriptor.xml.schema.application.URLsClassType;
import fr.gaellalire.vestige.application.manager.ApplicationDescriptor;
import fr.gaellalire.vestige.application.manager.ApplicationException;
import fr.gaellalire.vestige.application.manager.VersionUtils;
import fr.gaellalire.vestige.job.JobHelper;
import fr.gaellalire.vestige.platform.ClassLoaderConfiguration;
import fr.gaellalire.vestige.platform.JPMSClassLoaderConfiguration;
import fr.gaellalire.vestige.platform.ModuleConfiguration;
import fr.gaellalire.vestige.resolver.maven.MavenArtifactResolver;
import fr.gaellalire.vestige.resolver.maven.ResolveMode;
import fr.gaellalire.vestige.resolver.maven.Scope;
import fr.gaellalire.vestige.resolver.maven.ScopeModifier;

/**
 * @author Gael Lalire
 */
public class XMLApplicationDescriptor implements ApplicationDescriptor {

    private static final Pattern VERSION_RANGE_PATTERN = Pattern.compile("(\\d+)\\.(\\d+)\\.(\\d+)-(\\d+)");

    private MavenArtifactResolver mavenArtifactResolver;

    private List<Integer> version;

    private Application application;

    private MavenConfigResolved mavenConfigResolved;

    private Set<Permission> installerPermissions;

    private Set<Permission> permissions;

    private JobHelper actionHelper;

    private String javaSpecificationVersion;

    public XMLApplicationDescriptor(final MavenArtifactResolver mavenArtifactResolver, final String javaSpecificationVersion, final List<Integer> version,
            final Application application, final MavenConfigResolved mavenConfigResolved, final Set<Permission> permissions, final Set<Permission> installerPermissions,
            final JobHelper actionHelper) {
        this.mavenArtifactResolver = mavenArtifactResolver;
        this.javaSpecificationVersion = javaSpecificationVersion;
        this.version = version;
        this.application = application;
        this.mavenConfigResolved = mavenConfigResolved;
        this.permissions = permissions;
        this.installerPermissions = installerPermissions;
        this.actionHelper = actionHelper;
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
        MavenClassType mavenInstaller = installer.getMavenInstaller();
        if (mavenInstaller != null) {
            return mavenInstaller.getClazz();
        }
        URLsClassType urlsInstaller = installer.getUrlsInstaller();
        if (urlsInstaller != null) {
            return urlsInstaller.getClazz();
        }
        throw new ApplicationException("missing child");
    }

    public ClassLoaderConfiguration getInstallerClassLoaderConfiguration(final String configurationName) throws ApplicationException {
        Installer installer = application.getInstaller();
        if (installer == null) {
            return null;
        }
        URLsClassType urlsInstaller = installer.getUrlsInstaller();
        if (urlsInstaller != null) {
            List<String> url = urlsInstaller.getUrl();
            URL[] urls = new URL[url.size()];
            int i = 0;
            for (String string : url) {
                try {
                    urls[i] = new URL(string);
                } catch (MalformedURLException e) {
                    throw new ApplicationException("Not an URL", e);
                }
                i++;
            }
            URLClassLoaderConfigurationKey key;
            String name;
            if (urlsInstaller.getScope() == fr.gaellalire.vestige.application.descriptor.xml.schema.application.Scope.PLATFORM) {
                key = new URLClassLoaderConfigurationKey(true, urls);
                name = url.toString();
            } else {
                key = new URLClassLoaderConfigurationKey(false, urls);
                name = configurationName;
            }
            return new ClassLoaderConfiguration(key, name, urlsInstaller.getScope() == fr.gaellalire.vestige.application.descriptor.xml.schema.application.Scope.ATTACHMENT, urls,
                    Collections.<ClassLoaderConfiguration> emptyList(), null, null, null,
                    JPMSClassLoaderConfiguration.EMPTY_INSTANCE.merge(toModuleConfigurations(urlsInstaller.getAddExports(), urlsInstaller.getAddOpens())));
        }
        return resolve(configurationName, installer.getMavenInstaller(), actionHelper);
    }

    public static List<ModuleConfiguration> toModuleConfigurations(final List<fr.gaellalire.vestige.application.descriptor.xml.schema.application.ModulePackageName> addExports,
            final List<fr.gaellalire.vestige.application.descriptor.xml.schema.application.ModulePackageName> addOpens) {
        List<ModuleConfiguration> moduleConfigurations = new ArrayList<ModuleConfiguration>(addExports.size() + addOpens.size());
        for (fr.gaellalire.vestige.application.descriptor.xml.schema.application.ModulePackageName modulePackageName : addExports) {
            moduleConfigurations
                    .add(new ModuleConfiguration(modulePackageName.getModule(), Collections.singleton(modulePackageName.getPackage()), Collections.<String> emptySet()));
        }
        for (fr.gaellalire.vestige.application.descriptor.xml.schema.application.ModulePackageName modulePackageName : addOpens) {
            moduleConfigurations
                    .add(new ModuleConfiguration(modulePackageName.getModule(), Collections.<String> emptySet(), Collections.singleton(modulePackageName.getPackage())));
        }
        return moduleConfigurations;
    }

    public String getLauncherClassName() throws ApplicationException {
        Launcher launcher = application.getLauncher();
        MavenClassType mavenLauncher = launcher.getMavenLauncher();
        if (mavenLauncher != null) {
            return mavenLauncher.getClazz();
        }
        URLsClassType urlsLauncher = launcher.getUrlsLauncher();
        if (urlsLauncher != null) {
            return urlsLauncher.getClazz();
        }
        throw new ApplicationException("missing child");
    }

    public boolean isInstallerPrivateSystem() throws ApplicationException {
        Installer installer = application.getInstaller();
        if (installer == null) {
            return false;
        }
        return installer.isPrivateSystem();
    }

    public boolean isLauncherPrivateSystem() throws ApplicationException {
        Launcher launcher = application.getLauncher();
        if (launcher == null) {
            return false;
        }
        return launcher.isPrivateSystem();
    }

    public ClassLoaderConfiguration getLauncherClassLoaderConfiguration(final String configurationName) throws ApplicationException {
        Launcher launcher = application.getLauncher();
        URLsClassType urlsLauncher = launcher.getUrlsLauncher();
        if (urlsLauncher != null) {
            List<String> url = urlsLauncher.getUrl();
            URL[] urls = new URL[url.size()];
            int i = 0;
            for (String string : url) {
                try {
                    urls[i] = new URL(string);
                } catch (MalformedURLException e) {
                    throw new ApplicationException("Not an URL", e);
                }
                i++;
            }
            URLClassLoaderConfigurationKey key;
            String name;
            if (urlsLauncher.getScope() == fr.gaellalire.vestige.application.descriptor.xml.schema.application.Scope.PLATFORM) {
                key = new URLClassLoaderConfigurationKey(true, urls);
                name = url.toString();
            } else {
                key = new URLClassLoaderConfigurationKey(false, urls);
                name = configurationName;
            }
            return new ClassLoaderConfiguration(key, name, urlsLauncher.getScope() == fr.gaellalire.vestige.application.descriptor.xml.schema.application.Scope.ATTACHMENT, urls,
                    Collections.<ClassLoaderConfiguration> emptyList(), null, null, null,
                    JPMSClassLoaderConfiguration.EMPTY_INSTANCE.merge(toModuleConfigurations(urlsLauncher.getAddExports(), urlsLauncher.getAddOpens())));
        }
        return resolve(configurationName, launcher.getMavenLauncher(), actionHelper);
    }

    public static Scope convertScope(final fr.gaellalire.vestige.application.descriptor.xml.schema.application.Scope scope) throws ApplicationException {
        Scope mavenScope;
        switch (scope) {
        case ATTACHMENT:
            mavenScope = Scope.ATTACHMENT;
            break;
        case INSTALLATION:
            mavenScope = Scope.INSTALLATION;
            break;
        case PLATFORM:
            mavenScope = Scope.PLATFORM;
            break;
        default:
            throw new ApplicationException("Unknown scope " + scope);
        }
        return mavenScope;
    }

    public ClassLoaderConfiguration resolve(final String configurationName, final MavenClassType mavenClassType, final JobHelper actionHelper) throws ApplicationException {
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

        ScopeModifier scopeModifier = null;
        List<ModifyScope> modifyScopeList = mavenClassType.getModifyScope();
        if (modifyScopeList.size() != 0) {
            scopeModifier = new ScopeModifier();
            for (ModifyScope modifyScope : modifyScopeList) {
                scopeModifier.put(modifyScope.getGroupId(), modifyScope.getArtifactId(), convertScope(modifyScope.getScope()));
            }
        }

        try {
            return mavenArtifactResolver.resolve(configurationName, mavenClassType.getGroupId(), mavenClassType.getArtifactId(), mavenClassType.getVersion(),
                    mavenConfigResolved.getAdditionalRepositories(), mavenConfigResolved.getDefaultDependencyModifier(), mavenConfigResolved.getDefaultJPMSConfiguration(),
                    resolveMode, convertScope(mavenClassType.getScope()),
                    scopeModifier, mavenConfigResolved.isSuperPomRepositoriesUsed(), mavenConfigResolved.isPomRepositoriesIgnored(), actionHelper);
        } catch (Exception e) {
            throw new ApplicationException("Unable to resolve", e);
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
