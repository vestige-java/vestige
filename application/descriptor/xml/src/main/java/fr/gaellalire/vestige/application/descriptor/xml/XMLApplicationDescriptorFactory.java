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
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.Permission;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.gaellalire.vestige.application.descriptor.xml.schema.AddDependency;
import fr.gaellalire.vestige.application.descriptor.xml.schema.AdditionalRepository;
import fr.gaellalire.vestige.application.descriptor.xml.schema.Application;
import fr.gaellalire.vestige.application.descriptor.xml.schema.Config;
import fr.gaellalire.vestige.application.descriptor.xml.schema.ExceptIn;
import fr.gaellalire.vestige.application.descriptor.xml.schema.MavenConfig;
import fr.gaellalire.vestige.application.descriptor.xml.schema.ModifyDependency;
import fr.gaellalire.vestige.application.descriptor.xml.schema.ObjectFactory;
import fr.gaellalire.vestige.application.descriptor.xml.schema.Permissions;
import fr.gaellalire.vestige.application.descriptor.xml.schema.ReplaceDependency;
import fr.gaellalire.vestige.application.manager.ApplicationDescriptor;
import fr.gaellalire.vestige.application.manager.ApplicationDescriptorFactory;
import fr.gaellalire.vestige.application.manager.ApplicationException;
import fr.gaellalire.vestige.application.manager.VersionUtils;
import fr.gaellalire.vestige.resolver.maven.DefaultDependencyModifier;
import fr.gaellalire.vestige.resolver.maven.MavenArtifactResolver;
import fr.gaellalire.vestige.resolver.maven.MavenRepository;

/**
 * @author Gael Lalire
 */
public class XMLApplicationDescriptorFactory implements ApplicationDescriptorFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(XMLApplicationDescriptorFactory.class);

    private MavenArtifactResolver mavenArtifactResolver;

    public XMLApplicationDescriptorFactory(final MavenArtifactResolver mavenArtifactResolver) {
        this.mavenArtifactResolver = mavenArtifactResolver;
    }

    public boolean hasApplicationDescriptor(final URL context, final String repoName, final String appName, final List<Integer> version) throws ApplicationException {
        URL url;
        try {
            url = new URL(context, appName + "/" + appName + "-" + VersionUtils.toString(version) + ".xml");
        } catch (MalformedURLException e) {
            throw new ApplicationException("url is invalid", e);
        }
        try {
            URLConnection openConnection = url.openConnection();
            openConnection.connect();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    public ApplicationDescriptor createApplicationDescriptor(final URL context, final String repoName, final String appName, final List<Integer> version)
            throws ApplicationException {
        URL url;
        try {
            url = new URL(context, appName + "/" + appName + "-" + VersionUtils.toString(version) + ".xml");
        } catch (MalformedURLException e) {
            throw new ApplicationException("url repo issue", e);
        }

        Unmarshaller unMarshaller = null;
        try {
            JAXBContext jc = JAXBContext.newInstance(ObjectFactory.class.getPackage().getName());
            unMarshaller = jc.createUnmarshaller();

            URL xsdURL = XMLApplicationDescriptorFactory.class.getResource("application-1.0.0.xsd");
            SchemaFactory schemaFactory = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
            Schema schema = schemaFactory.newSchema(xsdURL);
            unMarshaller.setSchema(schema);
        } catch (Exception e) {
            throw new ApplicationException("Unable to initialize settings parser", e);
        }
        Application application;
        try {
            application = ((JAXBElement<Application>) unMarshaller.unmarshal(url)).getValue();
        } catch (JAXBException e) {
            throw new ApplicationException("unable to unmarshall application xml", e);
        }

        Config configurations = application.getConfigurations();
        Set<Permission> installerPermissionSet = new HashSet<Permission>();
        Set<Permission> launcherPermissionSet = new HashSet<Permission>();
        MavenConfigResolved mavenConfigResolved;
        if (configurations != null) {
            MavenConfig mavenConfig = configurations.getMavenConfig();
            if (mavenConfig != null) {
                mavenConfigResolved = resolveMavenConfig(configurations.getMavenConfig());
            } else {
                mavenConfigResolved = new MavenConfigResolved();
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
            mavenConfigResolved = new MavenConfigResolved();
        }
        return new XMLApplicationDescriptor(mavenArtifactResolver, repoName + "-" + appName + "-" + VersionUtils.toString(version), version, application, mavenConfigResolved, launcherPermissionSet);
    }

    public void readPermissions(final Permissions permissions, final Set<Permission> result) {
        ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
        for (fr.gaellalire.vestige.application.descriptor.xml.schema.Permission perm : permissions.getPermission()) {
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

    public MavenConfigResolved resolveMavenConfig(final MavenConfig mavenConfig) {
        List<MavenRepository> additionalRepositories = new ArrayList<MavenRepository>();
        DefaultDependencyModifier defaultDependencyModifier = new DefaultDependencyModifier();
        for (Object object : mavenConfig.getModifyDependencyOrReplaceDependencyOrAdditionalRepository()) {
            if (object instanceof ModifyDependency) {
                ModifyDependency modifyDependency = (ModifyDependency) object;
                List<AddDependency> addDependencies = modifyDependency.getAddDependency();
                List<Dependency> dependencies = new ArrayList<Dependency>(addDependencies.size());
                for (AddDependency addDependency : addDependencies) {
                    dependencies.add(new Dependency(new DefaultArtifact(addDependency.getGroupId(), addDependency.getArtifactId(), "jar", addDependency.getVersion()), "runtime"));
                }
                defaultDependencyModifier.add(modifyDependency.getGroupId(), modifyDependency.getArtifactId(), dependencies);
            } else if (object instanceof ReplaceDependency) {
                ReplaceDependency replaceDependency = (ReplaceDependency) object;
                List<AddDependency> addDependencies = replaceDependency.getAddDependency();
                List<Dependency> dependencies = new ArrayList<Dependency>(addDependencies.size());
                for (AddDependency addDependency : addDependencies) {
                    dependencies.add(new Dependency(new DefaultArtifact(addDependency.getGroupId(), addDependency.getArtifactId(), "jar", addDependency.getVersion()), "runtime"));
                }
                Map<String, Set<String>> exceptsMap = null;
                List<ExceptIn> excepts = replaceDependency.getExceptIn();
                if (excepts != null) {
                    exceptsMap = new HashMap<String, Set<String>>();
                    for (ExceptIn except : excepts) {
                        Set<String> set = exceptsMap.get(except.getGroupId());
                        if (set == null) {
                            set = new HashSet<String>();
                            exceptsMap.put(except.getGroupId(), set);
                        }
                        set.add(except.getArtifactId());
                    }
                }
                defaultDependencyModifier.replace(replaceDependency.getGroupId(), replaceDependency.getArtifactId(), dependencies, exceptsMap);
            } else if (object instanceof AdditionalRepository) {
                AdditionalRepository additionalRepository = (AdditionalRepository) object;
                additionalRepositories.add(new MavenRepository(additionalRepository.getId(), additionalRepository.getLayout(), additionalRepository.getUrl()));
            }
        }
        return new MavenConfigResolved(mavenConfig.isSuperPomRepositoriesUsed(), mavenConfig.isPomRepositoriesIgnored(), additionalRepositories, defaultDependencyModifier);
    }

}
