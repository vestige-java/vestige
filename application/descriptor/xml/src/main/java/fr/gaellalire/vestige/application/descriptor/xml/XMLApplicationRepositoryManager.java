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

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.Permission;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.gaellalire.vestige.application.descriptor.xml.schema.application.AddDependency;
import fr.gaellalire.vestige.application.descriptor.xml.schema.application.AdditionalRepository;
import fr.gaellalire.vestige.application.descriptor.xml.schema.application.Application;
import fr.gaellalire.vestige.application.descriptor.xml.schema.application.Config;
import fr.gaellalire.vestige.application.descriptor.xml.schema.application.ExceptIn;
import fr.gaellalire.vestige.application.descriptor.xml.schema.application.MavenConfig;
import fr.gaellalire.vestige.application.descriptor.xml.schema.application.ModifyDependency;
import fr.gaellalire.vestige.application.descriptor.xml.schema.application.ModulePackageName;
import fr.gaellalire.vestige.application.descriptor.xml.schema.application.ObjectFactory;
import fr.gaellalire.vestige.application.descriptor.xml.schema.application.Permissions;
import fr.gaellalire.vestige.application.descriptor.xml.schema.application.ReplaceDependency;
import fr.gaellalire.vestige.application.descriptor.xml.schema.repository.Repository;
import fr.gaellalire.vestige.application.descriptor.xml.schema.repository.Repository.Application.Version;
import fr.gaellalire.vestige.application.manager.ApplicationDescriptor;
import fr.gaellalire.vestige.application.manager.ApplicationException;
import fr.gaellalire.vestige.application.manager.ApplicationRepositoryManager;
import fr.gaellalire.vestige.application.manager.ApplicationRepositoryMetadata;
import fr.gaellalire.vestige.application.manager.CompatibilityChecker;
import fr.gaellalire.vestige.application.manager.URLOpener;
import fr.gaellalire.vestige.application.manager.VersionUtils;
import fr.gaellalire.vestige.spi.job.JobHelper;
import fr.gaellalire.vestige.spi.job.TaskHelper;
import fr.gaellalire.vestige.spi.resolver.maven.MavenContext;
import fr.gaellalire.vestige.spi.resolver.maven.MavenContextBuilder;
import fr.gaellalire.vestige.spi.resolver.maven.ModifyDependencyRequest;
import fr.gaellalire.vestige.spi.resolver.maven.ReplaceDependencyRequest;
import fr.gaellalire.vestige.spi.resolver.maven.VestigeMavenResolver;
import fr.gaellalire.vestige.spi.resolver.url_list.VestigeURLListResolver;

/**
 * @author Gael Lalire
 */
public class XMLApplicationRepositoryManager implements ApplicationRepositoryManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(XMLApplicationRepositoryManager.class);

    private VestigeURLListResolver vestigeURLListResolver;

    private VestigeMavenResolver vestigeMavenResolver;

    private int vestigeURLListResolverIndex;

    private int vestigeMavenResolverIndex;

    private URLOpener opener;

    public XMLApplicationRepositoryManager(final VestigeURLListResolver vestigeURLListResolver, final int vestigeURLListResolverIndex,
            final VestigeMavenResolver vestigeMavenResolver, final int vestigeMavenResolverIndex, final URLOpener opener) {
        this.vestigeURLListResolver = vestigeURLListResolver;
        this.vestigeURLListResolverIndex = vestigeURLListResolverIndex;
        this.vestigeMavenResolver = vestigeMavenResolver;
        this.vestigeMavenResolverIndex = vestigeMavenResolverIndex;
        this.opener = opener;
    }

    public int getVestigeMavenResolverIndex() {
        return vestigeMavenResolverIndex;
    }

    public int getVestigeURLListResolverIndex() {
        return vestigeURLListResolverIndex;
    }

    public boolean hasApplicationDescriptor(final URL context, final String repoName, final String appName, final List<Integer> version,
            final CompatibilityChecker compatibilityChecker) throws ApplicationException {
        URL url;
        try {
            url = new URL(context, appName + "/" + appName + "-" + VersionUtils.toString(version) + ".xml");
        } catch (MalformedURLException e) {
            throw new ApplicationException("url is invalid", e);
        }
        InputStream inputStream;
        try {
            inputStream = opener.openURL(url);
        } catch (IOException e) {
            LOGGER.trace("Cannot fetch application descriptor", e);
            return false;
        }
        try {
            if (compatibilityChecker.isJavaSpecificationVersionCompatible(getApplication(inputStream).getJavaSpecificationVersion())) {
                return true;
            }
            return false;
        } catch (JAXBException e) {
            LOGGER.trace("Exception confirms that url has no application descriptor (or an invalid one)", e);
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    public Application getApplication(final InputStream inputStream) throws ApplicationException, JAXBException {
        Unmarshaller unMarshaller = null;
        try {
            JAXBContext jc = JAXBContext.newInstance(ObjectFactory.class.getPackage().getName());
            unMarshaller = jc.createUnmarshaller();

            URL xsdURL = XMLApplicationRepositoryManager.class.getResource("application-1.0.0.xsd");
            SchemaFactory schemaFactory = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
            Schema schema = schemaFactory.newSchema(xsdURL);
            unMarshaller.setSchema(schema);
        } catch (Exception e) {
            throw new ApplicationException("Unable to initialize settings parser", e);
        }
        return ((JAXBElement<Application>) unMarshaller.unmarshal(inputStream)).getValue();
    }

    public ApplicationDescriptor createApplicationDescriptor(final URL context, final String repoName, final String appName, final List<Integer> version, final JobHelper jobHelper)
            throws ApplicationException {
        TaskHelper task = jobHelper.addTask("Reading application descriptor");
        URL url;
        try {
            url = new URL(context, appName + "/" + appName + "-" + VersionUtils.toString(version) + ".xml");
        } catch (MalformedURLException e) {
            throw new ApplicationException("URL repo issue", e);
        }

        InputStream inputStream;
        try {
            inputStream = opener.openURL(url);
        } catch (IOException e) {
            throw new ApplicationException("Cannot connect to repo URL", e);
        }

        Application application;
        try {
            application = getApplication(inputStream);
        } catch (JAXBException e) {
            throw new ApplicationException("Unable to unmarshall application xml", e);
        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                LOGGER.warn("Unable to close inputStream", e);
            }
        }
        task.setDone();

        Config configurations = application.getConfigurations();
        String javaSpecificationVersion = application.getJavaSpecificationVersion();
        Set<Permission> installerPermissionSet = new HashSet<Permission>();
        Set<Permission> launcherPermissionSet = new HashSet<Permission>();
        MavenContext mavenConfigResolved;
        if (configurations != null) {
            MavenConfig mavenConfig = configurations.getMavenConfig();
            if (mavenConfig != null) {
                mavenConfigResolved = resolveMavenConfig(mavenConfig);
            } else {
                mavenConfigResolved = vestigeMavenResolver.createMavenContextBuilder().build();
            }
            Config.Permissions permissions = configurations.getPermissions();
            if (permissions != null) {
                readPermissions(permissions, installerPermissionSet);
                readPermissions(permissions, launcherPermissionSet);
                Permissions installerPerms = permissions.getInstaller();
                if (installerPerms != null) {
                    readPermissions(installerPerms, installerPermissionSet);
                }
                Permissions launcherPerms = permissions.getLauncher();
                if (launcherPerms != null) {
                    readPermissions(launcherPerms, launcherPermissionSet);
                }
            }
        } else {
            mavenConfigResolved = vestigeMavenResolver.createMavenContextBuilder().build();
        }
        return new XMLApplicationDescriptor(this, javaSpecificationVersion, version, application, mavenConfigResolved, launcherPermissionSet, installerPermissionSet, jobHelper);
    }

    public VestigeURLListResolver getVestigeURLListResolver() {
        return vestigeURLListResolver;
    }

    public void readPermissions(final Permissions permissions, final Set<Permission> result) {
        ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
        for (fr.gaellalire.vestige.application.descriptor.xml.schema.application.Permission perm : permissions.getPermission()) {
            try {
                String type = perm.getType();
                String name = perm.getName();
                String actions = perm.getActions();
                Class<?> loadClass = systemClassLoader.loadClass(type);
                if (name == null) {
                    try {
                        result.add((Permission) loadClass.newInstance());
                        continue;
                    } catch (Exception e) {
                        LOGGER.trace("Exception", e);
                    }
                }
                if (actions == null) {
                    try {
                        result.add((Permission) loadClass.getConstructor(String.class).newInstance(name));
                        continue;
                    } catch (Exception e) {
                        LOGGER.trace("Exception", e);
                    }
                }
                try {
                    result.add((Permission) loadClass.getConstructor(String.class, String.class).newInstance(name, actions));
                    continue;
                } catch (Exception e) {
                    LOGGER.trace("Exception", e);
                }
                LOGGER.error("Permission issue");
            } catch (Exception e) {
                LOGGER.error("Permission issue", e);
            }
        }
    }

    public MavenContext resolveMavenConfig(final MavenConfig mavenConfig) {
        MavenContextBuilder mavenResolverRequestContext = vestigeMavenResolver.createMavenContextBuilder();

        for (Object object : mavenConfig.getModifyDependencyOrReplaceDependencyOrAdditionalRepository()) {
            if (object instanceof ModifyDependency) {
                ModifyDependency modifyDependency = (ModifyDependency) object;
                ModifyDependencyRequest modifyDependencyRequest = mavenResolverRequestContext.addModifyDependency(modifyDependency.getGroupId(), modifyDependency.getArtifactId());
                List<AddDependency> addDependencies = modifyDependency.getAddDependency();
                for (AddDependency addDependency : addDependencies) {
                    modifyDependencyRequest.addDependency(addDependency.getGroupId(), addDependency.getArtifactId(), addDependency.getVersion());
                }
                for (ModulePackageName addExports : modifyDependency.getAddExports()) {
                    modifyDependencyRequest.addExports(addExports.getModule(), addExports.getPackage());
                }
                for (ModulePackageName addExports : modifyDependency.getAddOpens()) {
                    modifyDependencyRequest.addOpens(addExports.getModule(), addExports.getPackage());
                }
                if (modifyDependency.getAddBeforeParent() != null) {
                    modifyDependencyRequest.setBeforeParent(true);
                }
                modifyDependencyRequest.execute();
            } else if (object instanceof ReplaceDependency) {
                ReplaceDependency replaceDependency = (ReplaceDependency) object;
                ReplaceDependencyRequest replaceDependencyRequest = mavenResolverRequestContext.addReplaceDependency(replaceDependency.getGroupId(),
                        replaceDependency.getArtifactId());
                List<AddDependency> addDependencies = replaceDependency.getAddDependency();
                for (AddDependency addDependency : addDependencies) {
                    replaceDependencyRequest.addDependency(addDependency.getGroupId(), addDependency.getArtifactId(), addDependency.getVersion());
                }
                List<ExceptIn> excepts = replaceDependency.getExceptIn();
                if (excepts != null) {
                    for (ExceptIn except : excepts) {
                        replaceDependencyRequest.addExcept(except.getGroupId(), except.getArtifactId());
                    }
                }
                replaceDependencyRequest.execute();
            } else if (object instanceof AdditionalRepository) {
                AdditionalRepository additionalRepository = (AdditionalRepository) object;
                mavenResolverRequestContext.addAdditionalRepository(additionalRepository.getId(), additionalRepository.getLayout(), additionalRepository.getUrl());
            }
        }
        mavenResolverRequestContext.setSuperPomRepositoriesUsed(mavenConfig.isSuperPomRepositoriesUsed());
        mavenResolverRequestContext.setPomRepositoriesIgnored(mavenConfig.isPomRepositoriesIgnored());
        return mavenResolverRequestContext.build();
    }

    @SuppressWarnings("unchecked")
    @Override
    public ApplicationRepositoryMetadata getMetadata(final URL context) {

        Map<String, Set<List<Integer>>> versionsByNames = new TreeMap<String, Set<List<Integer>>>();
        URL url;
        try {
            url = new URL(context, "repository.xml");
        } catch (MalformedURLException e) {
            LOGGER.warn("URL repo issue", e);
            return new XMLApplicationRepositoryMetadata(versionsByNames);
        }

        Unmarshaller unMarshaller = null;
        try {
            JAXBContext jc = JAXBContext.newInstance(fr.gaellalire.vestige.application.descriptor.xml.schema.repository.ObjectFactory.class.getPackage().getName());
            unMarshaller = jc.createUnmarshaller();

            URL xsdURL = XMLApplicationRepositoryManager.class.getResource("repository-1.0.0.xsd");
            SchemaFactory schemaFactory = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
            Schema schema = schemaFactory.newSchema(xsdURL);
            unMarshaller.setSchema(schema);
        } catch (Exception e) {
            LOGGER.warn("Unable to initialize repository parser", e);
            return new XMLApplicationRepositoryMetadata(versionsByNames);
        }

        InputStream inputStream;
        try {
            inputStream = opener.openURL(url);
        } catch (IOException e) {
            LOGGER.warn("Cannot connect to repo URL", e);
            return new XMLApplicationRepositoryMetadata(versionsByNames);
        }

        Repository repository;
        try {
            repository = ((JAXBElement<Repository>) unMarshaller.unmarshal(inputStream)).getValue();
        } catch (JAXBException e) {
            LOGGER.warn("Unable to unmarshall repository xml", e);
            return new XMLApplicationRepositoryMetadata(versionsByNames);
        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                LOGGER.warn("Unable to close inputStream", e);
            }
        }
        for (fr.gaellalire.vestige.application.descriptor.xml.schema.repository.Repository.Application application : repository.getApplication()) {
            Set<List<Integer>> versions = new TreeSet<List<Integer>>(VersionUtils.VERSION_COMPARATOR);
            for (Version v : application.getVersion()) {
                versions.add(VersionUtils.fromString(v.getValue()));
            }
            versionsByNames.put(application.getName(), versions);
        }
        return new XMLApplicationRepositoryMetadata(versionsByNames);

    }

}
