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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.net.MalformedURLException;
import java.net.ProxySelector;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.impl.client.SystemDefaultCredentialsProvider;
import org.apache.http.impl.conn.DefaultSchemePortResolver;
import org.apache.http.impl.conn.SystemDefaultRoutePlanner;
import org.apache.maven.model.Repository;
import org.apache.maven.model.building.DefaultModelBuilder;
import org.apache.maven.model.building.DefaultModelBuilderFactory;
import org.apache.maven.model.building.ModelBuilder;
import org.apache.maven.model.superpom.SuperPomProvider;
import org.apache.maven.repository.internal.ArtifactDescriptorUtils;
import org.apache.maven.repository.internal.DefaultArtifactDescriptorReader;
import org.apache.maven.repository.internal.DefaultVersionRangeResolver;
import org.apache.maven.repository.internal.DefaultVersionResolver;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.building.DefaultSettingsBuilder;
import org.apache.maven.settings.building.DefaultSettingsBuilderFactory;
import org.apache.maven.settings.building.DefaultSettingsBuildingRequest;
import org.apache.maven.settings.building.SettingsBuildingException;
import org.eclipse.aether.ConfigurationProperties;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.impl.ArtifactDescriptorReader;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.impl.DependencyCollector;
import org.eclipse.aether.impl.VersionRangeResolver;
import org.eclipse.aether.impl.VersionResolver;
import org.eclipse.aether.internal.impl.DefaultDependencyCollector;
import org.eclipse.aether.internal.impl.SimpleLocalRepositoryManagerFactory;
import org.eclipse.aether.repository.Authentication;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.NoLocalRepositoryManagerException;
import org.eclipse.aether.repository.Proxy;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.eclipse.aether.util.graph.visitor.PreorderNodeListGenerator;
import org.eclipse.aether.util.repository.AuthenticationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.gaellalire.vestige.core.url.DelegateURLStreamHandler;
import fr.gaellalire.vestige.core.url.VestigeURLStreamHandler;
import fr.gaellalire.vestige.jpms.NamedModuleUtils;
import fr.gaellalire.vestige.platform.ClassLoaderConfiguration;
import fr.gaellalire.vestige.platform.JPMSClassLoaderConfiguration;
import fr.gaellalire.vestige.platform.JPMSNamedModulesConfiguration;
import fr.gaellalire.vestige.platform.MinimalStringParserFactory;
import fr.gaellalire.vestige.platform.ModuleConfiguration;
import fr.gaellalire.vestige.platform.SecureFile;
import fr.gaellalire.vestige.platform.StringParserFactory;
import fr.gaellalire.vestige.platform.VestigePlatform;
import fr.gaellalire.vestige.platform.VestigeURLStreamHandlerFactory;
import fr.gaellalire.vestige.resolver.common.DefaultResolvedClassLoaderConfiguration;
import fr.gaellalire.vestige.spi.job.JobHelper;
import fr.gaellalire.vestige.spi.resolver.ResolvedClassLoaderConfiguration;
import fr.gaellalire.vestige.spi.resolver.ResolverException;
import fr.gaellalire.vestige.spi.resolver.Scope;
import fr.gaellalire.vestige.spi.resolver.maven.MavenContextBuilder;
import fr.gaellalire.vestige.spi.resolver.maven.VestigeMavenResolver;

/**
 * @author Gael Lalire
 */
public class MavenArtifactResolver implements VestigeMavenResolver {

    public static final String MVN_PROTOCOL = "mvn";

    public static final Pattern MVN_URL_PATTERN = Pattern.compile("mvn:([^/]*)/([^/]*)/([^/]*)(?:/([^/]*))?");

    public static final URLStreamHandler replaceMavenURLStreamHandler(final File baseDir, final VestigeURLStreamHandlerFactory vestigeURLStreamHandlerFactory) {
        DelegateURLStreamHandler delegateURLStreamHandler = vestigeURLStreamHandlerFactory.get(MVN_PROTOCOL);
        if (delegateURLStreamHandler == null) {
            delegateURLStreamHandler = new DelegateURLStreamHandler();
            vestigeURLStreamHandlerFactory.put(MVN_PROTOCOL, delegateURLStreamHandler);
        }

        VestigeURLStreamHandler vestigeURLStreamHandler = new VestigeURLStreamHandler(delegateURLStreamHandler) {

            @Override
            protected URLConnection openConnection(final URL u) throws IOException {
                Matcher matcher = MVN_URL_PATTERN.matcher(u.toExternalForm());
                if (!matcher.matches()) {
                    throw new IOException("Invalid Maven URL");
                }
                int i = 1;
                String groupId = matcher.group(i++);
                String artifactId = matcher.group(i++);
                String version = matcher.group(i++);
                String extension = matcher.group(i);
                if (extension == null) {
                    extension = "jar";
                }
                return new File(baseDir, groupId.replace('.', File.separatorChar) + File.separator + artifactId + File.separator + version + File.separator + artifactId + "-"
                        + version + "." + extension).toURI().toURL().openConnection();
            }
        };
        delegateURLStreamHandler.setDelegate(vestigeURLStreamHandler);
        return vestigeURLStreamHandler;
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(MavenArtifactResolver.class);

    // public static final URLStreamHandler URL_STREAM_HANDLER = new URLStreamHandler() {
    //
    // @Override
    // protected URLConnection openConnection(final URL u) throws IOException {
    // // FIXME must implement it for bouncy castle (JarVerifier)
    // // maybe no factory just handler by URL is better
    // // also the same file may have 2 different checksum to check, to verify when unserializing SecureFile (or make it transient ?)
    // throw new IOException("Unsupported operation");
    // }
    // };

    public File getBaseDir() {
        return localRepository.getBasedir();
    }

    private StringParserFactory stringParserFactory;

    private RepositorySystem repoSystem;

    private DependencyCollector dependencyCollector;

    private ArtifactDescriptorReader descriptorReader;

    private Proxy proxy;

    private List<RemoteRepository> superPomRemoteRepositories;

    private boolean offline;

    private LocalRepository localRepository;

    private SimpleLocalRepositoryManagerFactory simpleLocalRepositoryManagerFactory = new SimpleLocalRepositoryManagerFactory();

    // Don't know why it was introduce, provoke unwanted download because excluded dependencies were read anyway
    // I think it was because of the collectDependencies after the first one which was replaced by direct descriptorReader uses
    // private DependencySelector initialDependencySelector = new AndDependencySelector(new ScopeDependencySelector("test", "provided"), new OptionalDependencySelector());

    private VestigePlatform vestigePlatform;

    public MavenArtifactResolver(final VestigePlatform vestigePlatform, final File settingsFile) throws NoLocalRepositoryManagerException {
        this.vestigePlatform = vestigePlatform;
        stringParserFactory = new MinimalStringParserFactory();
        File localRepositoryFile = new File(System.getProperty("user.home"), ".m2" + File.separator + "repository");
        try {
            DefaultSettingsBuilder defaultSettingsBuilder = new DefaultSettingsBuilderFactory().newInstance();
            DefaultSettingsBuildingRequest request = new DefaultSettingsBuildingRequest();
            request.setUserSettingsFile(settingsFile);
            Settings settings = defaultSettingsBuilder.build(request).getEffectiveSettings();
            org.apache.maven.settings.Proxy activeProxy = settings.getActiveProxy();
            if (activeProxy != null) {
                Authentication authentication = null;
                String username = activeProxy.getUsername();
                if (username != null) {
                    authentication = new AuthenticationBuilder().addUsername(activeProxy.getUsername()).addPassword(activeProxy.getPassword()).build();
                }
                proxy = new Proxy(activeProxy.getProtocol(), activeProxy.getHost(), activeProxy.getPort(), authentication);
                LOGGER.info("Use proxy in Maven settings with id {}", activeProxy.getId());
            } else {
                LOGGER.info("No proxy in Maven settings, system proxy will be used (if any)");
            }
            String settingsLocalRepository = settings.getLocalRepository();
            if (settingsLocalRepository != null && settingsLocalRepository.length() != 0) {
                localRepositoryFile = new File(settingsLocalRepository);
            }
            offline = settings.isOffline();
        } catch (SettingsBuildingException e) {
            LOGGER.warn("Unable to read settings.xml, use default values", e);
        }
        LOGGER.info("Use m2 repository {}", localRepositoryFile);

        this.localRepository = new LocalRepository(localRepositoryFile);

        DefaultServiceLocator locator = new DefaultServiceLocator();
        locator.addService(VersionResolver.class, DefaultVersionResolver.class);
        locator.addService(VersionRangeResolver.class, DefaultVersionRangeResolver.class);
        locator.addService(ArtifactDescriptorReader.class, DefaultArtifactDescriptorReader.class);
        locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);

        HttpTransporterFactory httpTransporterFactory = new HttpTransporterFactory();
        httpTransporterFactory.setDefaultCredentialsProvider(new SystemDefaultCredentialsProvider());
        httpTransporterFactory.setDefaultRoutePlanner(new SystemDefaultRoutePlanner(DefaultSchemePortResolver.INSTANCE, ProxySelector.getDefault()));
        locator.setServices(TransporterFactory.class, httpTransporterFactory, new FileTransporterFactory());
        locator.setService(DependencyCollector.class, DefaultDependencyCollector.class);

        repoSystem = locator.getService(RepositorySystem.class);

        dependencyCollector = locator.getService(DependencyCollector.class);
        descriptorReader = locator.getService(ArtifactDescriptorReader.class);

        // read central repositories
        final SuperPomProvider[] superPomProviderArray = new SuperPomProvider[1];
        DefaultModelBuilderFactory defaultModelBuilderFactory = new DefaultModelBuilderFactory() {
            protected SuperPomProvider newSuperPomProvider() {
                superPomProviderArray[0] = super.newSuperPomProvider();
                return superPomProviderArray[0];
            }
        };
        DefaultModelBuilder defaultModelBuilder = defaultModelBuilderFactory.newInstance();
        locator.setServices(ModelBuilder.class, defaultModelBuilder);
        List<Repository> repositories = superPomProviderArray[0].getSuperModel("4.0.0").getRepositories();
        superPomRemoteRepositories = new ArrayList<RemoteRepository>(repositories.size());
        for (Repository repository : repositories) {
            String url = repository.getUrl();
            RemoteRepository.Builder repositoryBuilder = new RemoteRepository.Builder(repository.getId(), repository.getLayout(), url);
            repositoryBuilder.setSnapshotPolicy(ArtifactDescriptorUtils.toRepositoryPolicy(repository.getSnapshots()));
            repositoryBuilder.setReleasePolicy(ArtifactDescriptorUtils.toRepositoryPolicy(repository.getReleases()));
            if (proxy != null) {
                repositoryBuilder.setProxy(proxy);
            }

            superPomRemoteRepositories.add(repositoryBuilder.build());
        }
    }

    public DefaultRepositorySystemSession createSession(final JobHelper actionHelper) throws ResolverException {
        DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
        session.setTransferListener(new VestigeTransferListener(actionHelper));
        session.setOffline(offline);
        try {
            session.setLocalRepositoryManager(simpleLocalRepositoryManagerFactory.newInstance(session, localRepository));
        } catch (NoLocalRepositoryManagerException e) {
            throw new ResolverException(e);
        }
        // user agent may prevent NTLM access without authentification
        session.setConfigProperty(ConfigurationProperties.USER_AGENT, "Apache-HttpClient/4.5.3");
        return session;
    }

    public String getSha1(final Artifact artifact, final boolean firstTry) throws ResolverException {
        File file = artifact.getFile();
        File checksumFile = new File(file.getParentFile(), file.getName() + ".sha1");
        if (!file.exists() || !checksumFile.exists()) {
            if (!firstTry) {
                throw new ResolverException("Unable to get SHA-1 for " + artifact);
            }
            File[] listFiles = file.getParentFile().listFiles();
            for (File sfile : listFiles) {
                // clean dir
                sfile.delete();
            }
            return null;
        }

        String sha1;

        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(checksumFile));
            try {
                sha1 = bufferedReader.readLine();
            } finally {
                bufferedReader.close();
            }
        } catch (IOException e) {
            throw new ResolverException("Could not read SHA-1 file for " + artifact, e);
        }

        try {
            if (!sha1.equals(SecureFile.createChecksum(file))) {
                if (!firstTry) {
                    throw new ResolverException("SHA-1 did not match for " + artifact);
                }
                File[] listFiles = file.getParentFile().listFiles();
                for (File sfile : listFiles) {
                    // clean dir
                    sfile.delete();
                }
                return null;
            }
            return sha1;
        } catch (IOException e) {
            throw new ResolverException("Unable to compute checksum for " + artifact, e);
        } catch (NoSuchAlgorithmException e) {
            throw new ResolverException("Unable to compute checksum for " + artifact, e);
        }
    }

    public ClassLoaderConfiguration resolve(final String appName, final String groupId, final String artifactId, final String version, final String extension,
            final List<MavenRepository> additionalRepositories, final DefaultDependencyModifier dependencyModifier, final DefaultJPMSConfiguration jpmsConfiguration,
            final JPMSNamedModulesConfiguration jpmsNamedModulesConfiguration, final boolean manyLoaders, final Scope scope, final ScopeModifier scopeModifier,
            final boolean useSuperPomRepositories, final boolean ignorePomRepositories, final boolean checksum, final JobHelper actionHelper) throws ResolverException {

        Map<String, Map<String, MavenArtifact>> runtimeDependencies = new HashMap<String, Map<String, MavenArtifact>>();

        Dependency dependency = new Dependency(new DefaultArtifact(groupId, artifactId, extension, version), "runtime");

        CollectRequest collectRequest = new CollectRequest();
        collectRequest.setRoot(dependency);
        if (useSuperPomRepositories) {
            collectRequest.setRepositories(new ArrayList<RemoteRepository>(superPomRemoteRepositories));
        }
        for (MavenRepository additionalRepository : additionalRepositories) {
            RemoteRepository.Builder repositoryBuilder = new RemoteRepository.Builder(additionalRepository.getId(), additionalRepository.getLayout(),
                    additionalRepository.getUrl());
            if (proxy != null) {
                repositoryBuilder.setProxy(proxy);
            }
            collectRequest.addRepository(repositoryBuilder.build());
        }

        DefaultRepositorySystemSession session = createSession(actionHelper);

        session.setIgnoreArtifactDescriptorRepositories(ignorePomRepositories);

        boolean firstTry = true;
        doresolve: while (true) {

            LOGGER.info("Collecting dependencies for {}", appName);
            DependencyNode node;
            try {
                node = dependencyCollector.collectDependencies(session, collectRequest, dependencyModifier).getRoot();
            } catch (DependencyCollectionException e) {
                throw new ResolverException("Dependencies collection failed", e);
            }

            DependencyRequest dependencyRequest = new DependencyRequest(node, null);

            try {
                repoSystem.resolveDependencies(session, dependencyRequest);
            } catch (DependencyResolutionException e) {
                throw new ResolverException("Dependencies resolving failed", e);
            }

            PreorderNodeListGenerator nlg = new PreorderNodeListGenerator();
            node.accept(nlg);

            boolean hasNullSha1 = false;

            List<Artifact> artifacts = nlg.getArtifacts(true);
            LOGGER.info("Dependencies collected");

            LOGGER.info("Creating classloader configuration for {}", appName);
            ClassLoaderConfiguration classLoaderConfiguration;

            if (manyLoaders) {
                Map<MavenArtifact, File> urlByKey = new HashMap<MavenArtifact, File>();
                for (Artifact artifact : artifacts) {
                    Map<String, MavenArtifact> map = runtimeDependencies.get(artifact.getGroupId());
                    if (map == null) {
                        map = new HashMap<String, MavenArtifact>();
                        runtimeDependencies.put(artifact.getGroupId(), map);
                    }
                    String sha1 = null;
                    if (checksum) {
                        sha1 = getSha1(artifact, firstTry);
                        if (sha1 == null) {
                            hasNullSha1 = true;
                        }
                        if (hasNullSha1) {
                            // stop here we have to redownload
                            continue;
                        }
                    }
                    MavenArtifact mavenArtifact = new MavenArtifact(artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion(), artifact.getExtension(), sha1);
                    map.put(artifact.getArtifactId(), mavenArtifact);
                    urlByKey.put(mavenArtifact, artifact.getFile());
                }
                if (hasNullSha1) {
                    firstTry = false;
                    continue doresolve;
                }

                ClassLoaderConfigurationGraphHelper classLoaderConfigurationGraphHelper = new ClassLoaderConfigurationGraphHelper(appName, urlByKey, descriptorReader,
                        collectRequest, session, dependencyModifier, jpmsConfiguration, runtimeDependencies, scope, scopeModifier, jpmsNamedModulesConfiguration);

                GraphCycleRemover<NodeAndState, MavenArtifact, ClassLoaderConfigurationFactory> graphCycleRemover = new GraphCycleRemover<NodeAndState, MavenArtifact, ClassLoaderConfigurationFactory>(
                        classLoaderConfigurationGraphHelper);
                ClassLoaderConfigurationFactory removeCycle = graphCycleRemover.removeCycle(new NodeAndState(null, node, session.getDependencyManager()));
                if (jpmsNamedModulesConfiguration != null) {
                    removeCycle.setNamedModulesConfiguration(jpmsNamedModulesConfiguration);
                }
                classLoaderConfiguration = removeCycle.create(stringParserFactory);
            } else {
                List<SecureFile> beforeUrls = new ArrayList<SecureFile>();
                List<SecureFile> afterUrls = new ArrayList<SecureFile>();
                List<MavenArtifact> mavenArtifacts = new ArrayList<MavenArtifact>();
                JPMSClassLoaderConfiguration moduleConfiguration = JPMSClassLoaderConfiguration.EMPTY_INSTANCE;
                boolean[] beforeParents = null;
                int i = 0;
                for (Artifact artifact : artifacts) {
                    String sha1 = null;
                    if (checksum) {
                        sha1 = getSha1(artifact, firstTry);
                        if (sha1 == null) {
                            hasNullSha1 = true;
                        }
                        if (hasNullSha1) {
                            // stop here we have to redownload
                            continue;
                        }
                    }
                    MavenArtifact mavenArtifact = new MavenArtifact(artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion(), artifact.getExtension(), sha1);
                    mavenArtifacts.add(mavenArtifact);
                    List<SecureFile> urls;
                    if (dependencyModifier.isBeforeParent(artifact.getGroupId(), artifact.getArtifactId())) {
                        urls = beforeUrls;
                        if (beforeParents == null) {
                            beforeParents = new boolean[artifacts.size()];
                        }
                        beforeParents[i] = true;
                    } else {
                        urls = afterUrls;
                    }
                    File file = artifact.getFile();
                    try {
                        urls.add(new SecureFile(file, new URL(mavenArtifact.toString()), mavenArtifact.getSha1sum()));
                    } catch (MalformedURLException e) {
                        throw new ResolverException("Unable to create Maven URL", e);
                    }

                    JPMSClassLoaderConfiguration unnamedClassLoaderConfiguration = jpmsConfiguration.getModuleConfiguration(artifact.getGroupId(), artifact.getArtifactId());
                    if (jpmsNamedModulesConfiguration != null) {
                        String moduleName;
                        try {
                            moduleName = NamedModuleUtils.getModuleName(file);
                        } catch (IOException e) {
                            throw new ResolverException("Unable to calculate module name", e);
                        }

                        List<ModuleConfiguration> namedModuleConfigurations = new ArrayList<ModuleConfiguration>();
                        for (ModuleConfiguration unnamedModuleConfiguration : unnamedClassLoaderConfiguration.getModuleConfigurations()) {
                            namedModuleConfigurations.add(new ModuleConfiguration(unnamedModuleConfiguration.getModuleName(), unnamedModuleConfiguration.getAddExports(),
                                    unnamedModuleConfiguration.getAddOpens(), moduleName));
                        }
                        moduleConfiguration = moduleConfiguration.merge(namedModuleConfigurations);
                    } else {
                        moduleConfiguration = moduleConfiguration.merge(unnamedClassLoaderConfiguration);
                    }
                    i++;
                }
                if (hasNullSha1) {
                    firstTry = false;
                    continue doresolve;
                }
                MavenClassLoaderConfigurationKey key = new MavenClassLoaderConfigurationKey(mavenArtifacts, Collections.<MavenClassLoaderConfigurationKey> emptyList(), scope,
                        moduleConfiguration, jpmsNamedModulesConfiguration, beforeParents);
                String name;
                if (scope == Scope.PLATFORM) {
                    name = key.getArtifacts().toString();
                } else {
                    name = appName;
                }
                classLoaderConfiguration = new ClassLoaderConfiguration(key, name, scope == Scope.ATTACHMENT, beforeUrls, afterUrls,
                        Collections.<ClassLoaderConfiguration> emptyList(), null, null, null, null, key.getModuleConfiguration(), jpmsNamedModulesConfiguration);
            }
            LOGGER.info("Classloader configuration created");
            return classLoaderConfiguration;
        }
    }

    @Override
    public MavenContextBuilder createMavenContextBuilder() {
        return new MavenConfigResolved(this, vestigePlatform);
    }

    @Override
    public ResolvedClassLoaderConfiguration restoreSavedResolvedClassLoaderConfiguration(final ObjectInputStream objectInputStream) throws IOException {
        int size = objectInputStream.readInt();
        byte[] array = new byte[size];
        objectInputStream.readFully(array);
        try {
            ObjectInputStream internObjectInputStream = new ObjectInputStream(new ByteArrayInputStream(array)) {

                protected Class<?> resolveClass(final ObjectStreamClass desc) throws IOException, ClassNotFoundException {
                    return Class.forName(desc.getName(), false, MavenArtifactResolver.class.getClassLoader());
                }

            };
            boolean firstBeforeParent = internObjectInputStream.readBoolean();
            ClassLoaderConfiguration classLoaderConfiguration = (ClassLoaderConfiguration) internObjectInputStream.readObject();
            return new DefaultResolvedClassLoaderConfiguration(vestigePlatform, classLoaderConfiguration, firstBeforeParent);
        } catch (ClassNotFoundException e) {
            throw new IOException("ClassNotFoundException", e);
        }
    }

}
