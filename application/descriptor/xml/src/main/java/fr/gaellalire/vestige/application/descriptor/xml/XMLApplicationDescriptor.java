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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fr.gaellalire.vestige.application.descriptor.xml.schema.Application;
import fr.gaellalire.vestige.application.descriptor.xml.schema.Installer;
import fr.gaellalire.vestige.application.descriptor.xml.schema.Launcher;
import fr.gaellalire.vestige.application.descriptor.xml.schema.MavenClassType;
import fr.gaellalire.vestige.application.descriptor.xml.schema.Mode;
import fr.gaellalire.vestige.application.descriptor.xml.schema.URLsClassType;
import fr.gaellalire.vestige.application.manager.ApplicationDescriptor;
import fr.gaellalire.vestige.application.manager.ApplicationException;
import fr.gaellalire.vestige.application.manager.VersionUtils;
import fr.gaellalire.vestige.platform.ClassLoaderConfiguration;
import fr.gaellalire.vestige.resolver.maven.MavenArtifactResolver;
import fr.gaellalire.vestige.resolver.maven.ResolveMode;
import fr.gaellalire.vestige.resolver.maven.Scope;

/**
 * @author Gael Lalire
 */
public class XMLApplicationDescriptor implements ApplicationDescriptor {

    private static final Pattern VERSION_RANGE_PATTERN = Pattern.compile("(\\d+)\\.(\\d+)\\.(\\d+)-(\\d+)");

    private MavenArtifactResolver mavenArtifactResolver;

    private String appName;

    private List<Integer> version;

    private Application application;

    private MavenConfigResolved mavenConfigResolved;

    private Set<Permission> permissions;

    public XMLApplicationDescriptor(final MavenArtifactResolver mavenArtifactResolver, final String appName, final List<Integer> version, final Application application,
            final MavenConfigResolved mavenConfigResolved, final Set<Permission> permissions) {
        this.mavenArtifactResolver = mavenArtifactResolver;
        this.appName = appName;
        this.version = version;
        this.application = application;
        this.mavenConfigResolved = mavenConfigResolved;
        this.permissions = permissions;
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

    public ClassLoaderConfiguration getInstallerClassLoaderConfiguration() throws ApplicationException {
        Installer installer = application.getInstaller();
        if (installer == null) {
            return null;
        }
        String appName = this.appName + "-installer";
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
            if (urlsInstaller.getScope() == fr.gaellalire.vestige.application.descriptor.xml.schema.Scope.PLATFORM) {
                key = new URLClassLoaderConfigurationKey(true, urls);
                name = url.toString();
            } else {
                key = new URLClassLoaderConfigurationKey(false, urls);
                name = appName;
            }
            return new ClassLoaderConfiguration(key, name, urlsInstaller.getScope() == fr.gaellalire.vestige.application.descriptor.xml.schema.Scope.ATTACHMENT, urls,
                    Collections.<ClassLoaderConfiguration> emptyList(), null, null, null);
        }
        return resolve(appName, installer.getMavenInstaller());
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

    public ClassLoaderConfiguration getLauncherClassLoaderConfiguration() throws ApplicationException {
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
            if (urlsLauncher.getScope() == fr.gaellalire.vestige.application.descriptor.xml.schema.Scope.PLATFORM) {
                key = new URLClassLoaderConfigurationKey(true, urls);
                name = url.toString();
            } else {
                key = new URLClassLoaderConfigurationKey(false, urls);
                name = appName;
            }
            return new ClassLoaderConfiguration(key, name, urlsLauncher.getScope() == fr.gaellalire.vestige.application.descriptor.xml.schema.Scope.ATTACHMENT, urls,
                    Collections.<ClassLoaderConfiguration> emptyList(), null, null, null);
        }
        return resolve(appName, launcher.getMavenLauncher());
    }

    public ClassLoaderConfiguration resolve(final String appName, final MavenClassType mavenClassType) throws ApplicationException {
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

        Scope mavenScope;
        fr.gaellalire.vestige.application.descriptor.xml.schema.Scope scope = mavenClassType.getScope();
        switch (scope) {
        case ATTACHMENT:
            mavenScope = Scope.ATTACHMENT;
            break;
        case APPLICATION:
            mavenScope = Scope.APPLICATION;
            break;
        case PLATFORM:
            mavenScope = Scope.PLATFORM;
            break;
        default:
            throw new ApplicationException("Unknown scope " + mode);
        }

        try {
            return mavenArtifactResolver.resolve(appName, mavenClassType.getGroupId(), mavenClassType.getArtifactId(), mavenClassType.getVersion(), mavenConfigResolved.getAdditionalRepositories(),
                    mavenConfigResolved.getDefaultDependencyModifier(), resolveMode, mavenScope, mavenConfigResolved.isSuperPomRepositoriesUsed(), mavenConfigResolved.isPomRepositoriesIgnored());
        } catch (Exception e) {
            throw new ApplicationException("Unable to resolve", e);
        }
    }

    @Override
    public Set<Permission> getInstallerPermissions() throws ApplicationException {
        return permissions;
    }

    @Override
    public Set<Permission> getPermissions() throws ApplicationException {
        return permissions;
    }

}
