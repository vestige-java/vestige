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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
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
import fr.gaellalire.vestige.application.manager.PermissionFactory;
import fr.gaellalire.vestige.application.manager.PermissionSetFactory;
import fr.gaellalire.vestige.application.manager.RepositoryOverride;
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
import fr.gaellalire.vestige.utils.SimpleValueGetter;
import fr.gaellalire.vestige.utils.UtilsSchema;

/**
 * @author Gael Lalire
 */
public class XMLApplicationRepositoryManager implements ApplicationRepositoryManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(XMLApplicationRepositoryManager.class);

    private VestigeURLListResolver vestigeURLListResolver;

    private VestigeMavenResolver vestigeMavenResolver;

    private VestigeMavenResolver installVestigeMavenResolver;

    private int vestigeURLListResolverIndex;

    private int vestigeMavenResolverIndex;

    private URLOpener opener;

    public XMLApplicationRepositoryManager(final VestigeURLListResolver vestigeURLListResolver, final int vestigeURLListResolverIndex,
            final VestigeMavenResolver vestigeMavenResolver, final VestigeMavenResolver installVestigeMavenResolver, final int vestigeMavenResolverIndex, final URLOpener opener) {
        this.vestigeURLListResolver = vestigeURLListResolver;
        this.vestigeURLListResolverIndex = vestigeURLListResolverIndex;
        this.vestigeMavenResolver = vestigeMavenResolver;
        this.installVestigeMavenResolver = installVestigeMavenResolver;
        this.vestigeMavenResolverIndex = vestigeMavenResolverIndex;
        this.opener = opener;
    }

    public int getVestigeMavenResolverIndex() {
        return vestigeMavenResolverIndex;
    }

    public int getVestigeURLListResolverIndex() {
        return vestigeURLListResolverIndex;
    }

    public boolean hasApplicationDescriptor(final URL context, final String appName, final List<Integer> version, final CompatibilityChecker compatibilityChecker)
            throws ApplicationException {
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
            Application application = getApplication(inputStream);
            if (compatibilityChecker.isJavaSpecificationVersionCompatible(SimpleValueGetter.INSTANCE.getValue(application.getJavaSpecificationVersion()),
                    SimpleValueGetter.INSTANCE.getValue(application.getMaxJavaSpecificationVersion()))) {
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

            URL xsdURL = XMLApplicationRepositoryManager.class.getResource("application.xsd");
            SchemaFactory schemaFactory = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
            Schema schema = schemaFactory.newSchema(new Source[] {new StreamSource(UtilsSchema.getURL().toExternalForm()), new StreamSource(xsdURL.toExternalForm())});
            unMarshaller.setSchema(schema);
        } catch (Exception e) {
            throw new ApplicationException("Unable to initialize settings parser", e);
        }
        return ((JAXBElement<Application>) unMarshaller.unmarshal(inputStream)).getValue();
    }

    public ApplicationDescriptor createApplicationDescriptor(final RepositoryOverride repositoryOverride, final URL context, final String appName, final List<Integer> version,
            final JobHelper jobHelper) throws ApplicationException {
        TaskHelper task = jobHelper.addTask("Reading application descriptor");
        URL url = null;
        if (repositoryOverride != null) {
            url = repositoryOverride.getApplication();
        }
        if (url == null) {
            try {
                url = new URL(context, appName + "/" + appName + "-" + VersionUtils.toString(version) + ".xml");
            } catch (MalformedURLException e) {
                throw new ApplicationException("URL repo issue", e);
            }
        }

        Application application;
        try {
            InputStream inputStream = opener.openURL(url);
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
        } catch (IOException e) {
            throw new ApplicationException("Cannot connect to repo URL", e);
        }

        task.setDone();

        Config configurations = application.getConfigurations();
        String javaSpecificationVersion = SimpleValueGetter.INSTANCE.getValue(application.getJavaSpecificationVersion());
        String maxJavaSpecificationVersion = SimpleValueGetter.INSTANCE.getValue(application.getMaxJavaSpecificationVersion());
        PermissionSetFactory installerPermissionSet = new PermissionSetFactory();
        PermissionSetFactory launcherPermissionSet = new PermissionSetFactory();
        MavenContext mavenContext = createMavenContext(configurations, installerPermissionSet, launcherPermissionSet, vestigeMavenResolver);
        MavenContext installerMavenContext;
        if (installVestigeMavenResolver != null) {
            installerMavenContext = createMavenContext(configurations, installerPermissionSet, launcherPermissionSet, installVestigeMavenResolver);
        } else {
            installerMavenContext = mavenContext;
        }
        return new XMLApplicationDescriptor(this, javaSpecificationVersion, maxJavaSpecificationVersion, version, application, mavenContext, installerMavenContext,
                launcherPermissionSet, installerPermissionSet, jobHelper);
    }

    public MavenContext createMavenContext(final Config configurations, final PermissionSetFactory installerPermissionSet, final PermissionSetFactory launcherPermissionSet,
            final VestigeMavenResolver vestigeMavenResolver) {
        MavenContext mavenConfigResolved;
        if (configurations != null) {
            MavenConfig mavenConfig = configurations.getMavenConfig();
            if (mavenConfig != null) {
                mavenConfigResolved = resolveMavenConfig(mavenConfig, vestigeMavenResolver);
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
        return mavenConfigResolved;
    }

    public VestigeURLListResolver getVestigeURLListResolver() {
        return vestigeURLListResolver;
    }

    public void readPermissions(final Permissions permissions, final PermissionSetFactory result) {
        for (fr.gaellalire.vestige.application.descriptor.xml.schema.application.Permission perm : permissions.getPermission()) {
            result.addPermissionFactory(new PermissionFactory(perm.getType(), perm.getName(), perm.getActions()));
        }
    }

    public MavenContext resolveMavenConfig(final MavenConfig mavenConfig, final VestigeMavenResolver vestigeMavenResolver) {
        MavenContextBuilder mavenResolverRequestContext = vestigeMavenResolver.createMavenContextBuilder();
        for (Object object : mavenConfig.getModifyDependencyOrReplaceDependencyOrAdditionalRepository()) {
            if (object instanceof ModifyDependency) {
                ModifyDependency modifyDependency = (ModifyDependency) object;
                ModifyDependencyRequest modifyDependencyRequest = mavenResolverRequestContext.addModifyDependency(
                        SimpleValueGetter.INSTANCE.getValue(modifyDependency.getGroupId()), SimpleValueGetter.INSTANCE.getValue(modifyDependency.getArtifactId()));
                AddDependency patch = modifyDependency.getPatch();
                if (patch != null) {
                    modifyDependencyRequest.setPatch(SimpleValueGetter.INSTANCE.getValue(patch.getGroupId()), SimpleValueGetter.INSTANCE.getValue(patch.getArtifactId()),
                            SimpleValueGetter.INSTANCE.getValue(patch.getVersion()));
                }
                List<AddDependency> addDependencies = modifyDependency.getAddDependency();
                for (AddDependency addDependency : addDependencies) {
                    modifyDependencyRequest.addDependency(SimpleValueGetter.INSTANCE.getValue(addDependency.getGroupId()),
                            SimpleValueGetter.INSTANCE.getValue(addDependency.getArtifactId()), SimpleValueGetter.INSTANCE.getValue(addDependency.getVersion()));
                }
                modifyDependencyRequest.execute();
            } else if (object instanceof ReplaceDependency) {
                ReplaceDependency replaceDependency = (ReplaceDependency) object;
                ReplaceDependencyRequest replaceDependencyRequest = mavenResolverRequestContext.addReplaceDependency(
                        SimpleValueGetter.INSTANCE.getValue(replaceDependency.getGroupId()), SimpleValueGetter.INSTANCE.getValue(replaceDependency.getArtifactId()));
                List<AddDependency> addDependencies = replaceDependency.getAddDependency();
                for (AddDependency addDependency : addDependencies) {
                    replaceDependencyRequest.addDependency(SimpleValueGetter.INSTANCE.getValue(addDependency.getGroupId()),
                            SimpleValueGetter.INSTANCE.getValue(addDependency.getArtifactId()), SimpleValueGetter.INSTANCE.getValue(addDependency.getVersion()));
                }
                List<ExceptIn> excepts = replaceDependency.getExceptIn();
                if (excepts != null) {
                    for (ExceptIn except : excepts) {
                        replaceDependencyRequest.addExcept(SimpleValueGetter.INSTANCE.getValue(except.getGroupId()), SimpleValueGetter.INSTANCE.getValue(except.getArtifactId()));
                    }
                }
                replaceDependencyRequest.execute();
            } else if (object instanceof AdditionalRepository) {
                AdditionalRepository additionalRepository = (AdditionalRepository) object;
                mavenResolverRequestContext.addAdditionalRepository(SimpleValueGetter.INSTANCE.getValue(additionalRepository.getId()),
                        SimpleValueGetter.INSTANCE.getValue(additionalRepository.getLayout()), SimpleValueGetter.INSTANCE.getValue(additionalRepository.getUrl()));
            }
        }
        mavenResolverRequestContext.setSuperPomRepositoriesIgnored(SimpleValueGetter.INSTANCE.getValue(mavenConfig.getSuperPomRepositoriesUsed()));
        mavenResolverRequestContext.setPomRepositoriesIgnored(SimpleValueGetter.INSTANCE.getValue(mavenConfig.getPomRepositoriesIgnored()));
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

            URL xsdURL = XMLApplicationRepositoryManager.class.getResource("repository.xsd");
            SchemaFactory schemaFactory = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
            Schema schema = schemaFactory.newSchema(new Source[] {new StreamSource(UtilsSchema.getURL().toExternalForm()), new StreamSource(xsdURL.toExternalForm())});
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
                versions.add(VersionUtils.fromString(SimpleValueGetter.INSTANCE.getValue(v.getValue())));
            }
            versionsByNames.put(SimpleValueGetter.INSTANCE.getValue(application.getName()), versions);
        }
        return new XMLApplicationRepositoryMetadata(versionsByNames);

    }

}
