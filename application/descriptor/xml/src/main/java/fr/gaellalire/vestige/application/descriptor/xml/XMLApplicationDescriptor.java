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

import fr.gaellalire.vestige.application.descriptor.xml.schema.application.ActivateNamedModules;
import fr.gaellalire.vestige.application.descriptor.xml.schema.application.AddExports;
import fr.gaellalire.vestige.application.descriptor.xml.schema.application.AddOpens;
import fr.gaellalire.vestige.application.descriptor.xml.schema.application.AddReads;
import fr.gaellalire.vestige.application.descriptor.xml.schema.application.Application;
import fr.gaellalire.vestige.application.descriptor.xml.schema.application.Installer;
import fr.gaellalire.vestige.application.descriptor.xml.schema.application.Launcher;
import fr.gaellalire.vestige.application.descriptor.xml.schema.application.MavenClassType;
import fr.gaellalire.vestige.application.descriptor.xml.schema.application.Mode;
import fr.gaellalire.vestige.application.descriptor.xml.schema.application.ModifyScope;
import fr.gaellalire.vestige.application.descriptor.xml.schema.application.URLsClassType;
import fr.gaellalire.vestige.application.manager.ApplicationDescriptor;
import fr.gaellalire.vestige.application.manager.ApplicationException;
import fr.gaellalire.vestige.application.manager.ApplicationResolvedClassLoaderConfiguration;
import fr.gaellalire.vestige.application.manager.VersionUtils;
import fr.gaellalire.vestige.spi.job.JobHelper;
import fr.gaellalire.vestige.spi.resolver.ResolverException;
import fr.gaellalire.vestige.spi.resolver.Scope;
import fr.gaellalire.vestige.spi.resolver.maven.MavenContext;
import fr.gaellalire.vestige.spi.resolver.maven.ResolveMavenArtifactRequest;
import fr.gaellalire.vestige.spi.resolver.maven.ResolveMode;
import fr.gaellalire.vestige.spi.resolver.url_list.URLListRequest;

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

    public ApplicationResolvedClassLoaderConfiguration getInstallerClassLoaderConfiguration(final String configurationName) throws ApplicationException {
        Installer installer = application.getInstaller();
        if (installer == null) {
            return null;
        }
        URLsClassType urlsInstaller = installer.getUrlsInstaller();
        if (urlsInstaller != null) {
            URLListRequest urlListRequest = xmlApplicationRepositoryManager.getVestigeURLListResolver().createURLListRequest(convertScope(urlsInstaller.getScope()),
                    configurationName);

            for (String string : urlsInstaller.getUrl()) {
                try {
                    urlListRequest.addURL(new URL(string));
                } catch (MalformedURLException e) {
                    throw new ApplicationException("Not an URL", e);
                }
            }

            for (fr.gaellalire.vestige.application.descriptor.xml.schema.application.ModulePackageName modulePackageName : urlsInstaller.getAddExports()) {
                urlListRequest.addExports(modulePackageName.getModule(), modulePackageName.getPackage());
            }
            for (fr.gaellalire.vestige.application.descriptor.xml.schema.application.ModulePackageName modulePackageName : urlsInstaller.getAddOpens()) {
                urlListRequest.addOpens(modulePackageName.getModule(), modulePackageName.getPackage());
            }

            try {
                return new ApplicationResolvedClassLoaderConfiguration(urlListRequest.execute(jobHelper), xmlApplicationRepositoryManager.getVestigeURLListResolverIndex());
            } catch (ResolverException e) {
                throw new ApplicationException(e);
            }
        }
        return resolve(configurationName, installer.getMavenInstaller(), jobHelper);
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

    public ApplicationResolvedClassLoaderConfiguration getLauncherClassLoaderConfiguration(final String configurationName) throws ApplicationException {
        Launcher launcher = application.getLauncher();
        URLsClassType urlsLauncher = launcher.getUrlsLauncher();
        if (urlsLauncher != null) {
            URLListRequest urlListRequest = xmlApplicationRepositoryManager.getVestigeURLListResolver().createURLListRequest(convertScope(urlsLauncher.getScope()),
                    configurationName);

            for (String string : urlsLauncher.getUrl()) {
                try {
                    urlListRequest.addURL(new URL(string));
                } catch (MalformedURLException e) {
                    throw new ApplicationException("Not an URL", e);
                }
            }

            for (fr.gaellalire.vestige.application.descriptor.xml.schema.application.ModulePackageName modulePackageName : urlsLauncher.getAddExports()) {
                urlListRequest.addExports(modulePackageName.getModule(), modulePackageName.getPackage());
            }
            for (fr.gaellalire.vestige.application.descriptor.xml.schema.application.ModulePackageName modulePackageName : urlsLauncher.getAddOpens()) {
                urlListRequest.addOpens(modulePackageName.getModule(), modulePackageName.getPackage());
            }
            urlsLauncher.getScope();

            try {
                return new ApplicationResolvedClassLoaderConfiguration(urlListRequest.execute(jobHelper), xmlApplicationRepositoryManager.getVestigeURLListResolverIndex());
            } catch (ResolverException e) {
                throw new ApplicationException(e);
            }
        }
        return resolve(configurationName, launcher.getMavenLauncher(), jobHelper);
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

        ResolveMavenArtifactRequest resolveMavenArtifactRequest = mavenContext.resolve(resolveMode, convertScope(mavenClassType.getScope()), mavenClassType.getGroupId(),
                mavenClassType.getArtifactId(), mavenClassType.getVersion(), configurationName);

        for (ModifyScope modifyScope : mavenClassType.getModifyScope()) {
            resolveMavenArtifactRequest.addModifyScope(modifyScope.getGroupId(), modifyScope.getArtifactId(), convertScope(modifyScope.getScope()));
        }

        ActivateNamedModules activateNamedModules = mavenClassType.getActivateNamedModules();
        if (activateNamedModules != null) {
            resolveMavenArtifactRequest.setNamedModuleActivated(true);
            for (AddReads addReads : activateNamedModules.getAddReads()) {
                resolveMavenArtifactRequest.addReads(addReads.getSource(), addReads.getTarget());
            }
            for (AddExports addExports : activateNamedModules.getAddExports()) {
                resolveMavenArtifactRequest.addExports(addExports.getSource(), addExports.getPn(), addExports.getTarget());
            }
            for (AddOpens addOpens : activateNamedModules.getAddOpens()) {
                resolveMavenArtifactRequest.addOpens(addOpens.getSource(), addOpens.getPn(), addOpens.getTarget());
            }
        }

        try {
            return new ApplicationResolvedClassLoaderConfiguration(resolveMavenArtifactRequest.execute(actionHelper),
                    xmlApplicationRepositoryManager.getVestigeMavenResolverIndex());
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
