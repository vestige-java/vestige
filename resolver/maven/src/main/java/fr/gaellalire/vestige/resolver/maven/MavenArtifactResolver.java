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
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.impl.ArtifactDescriptorReader;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.impl.DependencyCollector;
import org.eclipse.aether.impl.DependencyModifier;
import org.eclipse.aether.impl.VersionRangeResolver;
import org.eclipse.aether.impl.VersionResolver;
import org.eclipse.aether.internal.impl.DefaultDependencyCollector;
import org.eclipse.aether.internal.impl.SimpleLocalRepositoryManagerFactory;
import org.eclipse.aether.repository.Authentication;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.NoLocalRepositoryManagerException;
import org.eclipse.aether.repository.Proxy;
import org.eclipse.aether.repository.ProxySelector;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.eclipse.aether.util.graph.visitor.PreorderNodeListGenerator;
import org.eclipse.aether.util.repository.AuthenticationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.gaellalire.vestige.job.JobHelper;
import fr.gaellalire.vestige.platform.ClassLoaderConfiguration;
import fr.gaellalire.vestige.platform.JPMSClassLoaderConfiguration;
import fr.gaellalire.vestige.platform.MinimalStringParserFactory;
import fr.gaellalire.vestige.platform.StringParserFactory;

/**
 * @author Gael Lalire
 */
public class MavenArtifactResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(MavenArtifactResolver.class);

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

    private ProxySelector proxySelector;

    public MavenArtifactResolver(final File settingsFile) throws NoLocalRepositoryManagerException {
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

        locator.addService(TransporterFactory.class, HttpTransporterFactory.class);
        locator.addService(TransporterFactory.class, FileTransporterFactory.class);
        locator.setService(DependencyCollector.class, DefaultDependencyCollector.class);

        repoSystem = locator.getService(RepositorySystem.class);

        dependencyCollector = locator.getService(DependencyCollector.class);
        descriptorReader = locator.getService(ArtifactDescriptorReader.class);

        proxySelector = new ProxySelector() {

            public Proxy getProxy(final RemoteRepository repository) {
                if (proxy == null) {
                    try {
                        return getSystemProxy(new URI(repository.getUrl()));
                    } catch (URISyntaxException e) {
                        LOGGER.error("URL is not valid", e);
                    }
                }
                return proxy;
            }
        };

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
            if (proxy == null) {
                try {
                    repositoryBuilder.setProxy(getSystemProxy(new URI(url)));
                } catch (URISyntaxException e) {
                    LOGGER.warn("URL of a super pom repository is invalid", e);
                    repositoryBuilder.setProxy(null);
                }
            } else {
                repositoryBuilder.setProxy(proxy);
            }

            superPomRemoteRepositories.add(repositoryBuilder.build());
        }
    }

    public DefaultRepositorySystemSession createSession(final JobHelper actionHelper) throws NoLocalRepositoryManagerException {
        DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
        session.setTransferListener(new VestigeTransferListener(actionHelper));
        session.setOffline(offline);
        session.setLocalRepositoryManager(simpleLocalRepositoryManagerFactory.newInstance(session, localRepository));
        // session.setDependencySelector(initialDependencySelector);

        session.setProxySelector(proxySelector);
        return session;
    }

    public static final List<String> NON_PROXY_URI_SCHEMES = Arrays.asList("file");

    public Proxy getSystemProxy(final URI uri) {
        if (NON_PROXY_URI_SCHEMES.contains(uri.getScheme())) {
            return null;
        }
        Iterator<java.net.Proxy> proxyIterator = java.net.ProxySelector.getDefault().select(uri).iterator();
        if (proxyIterator.hasNext()) {
            java.net.Proxy next = proxyIterator.next();
            if (next != java.net.Proxy.NO_PROXY) {
                SocketAddress address = next.address();
                if (address instanceof InetSocketAddress) {
                    InetSocketAddress inetSocketAddress = (InetSocketAddress) address;
                    Authentication authentication = null;

                    // FIXME : how to authent ?

                    // PasswordAuthentication requestPasswordAuthentication = Authenticator.requestPasswordAuthentication(
                    // inetSocketAddress.getHostName(), inetSocketAddress.getAddress(), inetSocketAddress.getPort(),
                    // uri.getScheme(), "", "NTLM");
                    // if (requestPasswordAuthentication != null) {
                    // String userName = requestPasswordAuthentication.getUserName();
                    // char[] password = requestPasswordAuthentication.getPassword();
                    // authentication = new Authentication(userName, new String(password));
                    // }
                    return new Proxy(uri.getScheme(), inetSocketAddress.getHostName(), inetSocketAddress.getPort(), authentication);
                }
            }
        }
        return null;
    }

    public ClassLoaderConfiguration resolve(final String appName, final String groupId, final String artifactId, final String version,
            final List<MavenRepository> additionalRepositories, final DependencyModifier dependencyModifier, final DefaultJPMSConfiguration jpmsConfiguration,
            final ResolveMode resolveMode, final Scope scope, final ScopeModifier scopeModifier, final boolean useSuperPomRepositories, final boolean ignorePomRepositories,
            final JobHelper actionHelper) throws Exception {

        Map<String, Map<String, MavenArtifact>> runtimeDependencies = new HashMap<String, Map<String, MavenArtifact>>();

        Dependency dependency = new Dependency(new DefaultArtifact(groupId, artifactId, "jar", version), "runtime");

        CollectRequest collectRequest = new CollectRequest();
        collectRequest.setRoot(dependency);
        if (useSuperPomRepositories) {
            collectRequest.setRepositories(new ArrayList<RemoteRepository>(superPomRemoteRepositories));
        }
        for (MavenRepository additionalRepository : additionalRepositories) {
            RemoteRepository.Builder repositoryBuilder = new RemoteRepository.Builder(additionalRepository.getId(), additionalRepository.getLayout(),
                    additionalRepository.getUrl());
            if (proxy == null) {
                repositoryBuilder.setProxy(getSystemProxy(new URI(additionalRepository.getUrl())));
            } else {
                repositoryBuilder.setProxy(proxy);
            }
            collectRequest.addRepository(repositoryBuilder.build());
        }

        DefaultRepositorySystemSession session = createSession(actionHelper);

        session.setIgnoreArtifactDescriptorRepositories(ignorePomRepositories);

        LOGGER.info("Collecting dependencies for {}", appName);
        DependencyNode node = dependencyCollector.collectDependencies(session, collectRequest, dependencyModifier).getRoot();

        DependencyRequest dependencyRequest = new DependencyRequest(node, null);

        repoSystem.resolveDependencies(session, dependencyRequest);

        PreorderNodeListGenerator nlg = new PreorderNodeListGenerator();
        node.accept(nlg);

        List<Artifact> artifacts = nlg.getArtifacts(true);
        LOGGER.info("Dependencies collected");

        LOGGER.info("Creating classloader configuration for {}", appName);
        ClassLoaderConfiguration classLoaderConfiguration;
        switch (resolveMode) {
        case CLASSPATH:
            URL[] urls = new URL[artifacts.size()];
            int i = 0;
            List<MavenArtifact> mavenArtifacts = new ArrayList<MavenArtifact>();
            JPMSClassLoaderConfiguration moduleConfiguration = JPMSClassLoaderConfiguration.EMPTY_INSTANCE;
            for (Artifact artifact : artifacts) {
                mavenArtifacts.add(new MavenArtifact(artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion()));
                moduleConfiguration = moduleConfiguration.merge(jpmsConfiguration.getModuleConfiguration(artifact.getGroupId(), artifact.getArtifactId()));
                urls[i] = artifact.getFile().toURI().toURL();
                i++;
            }
            MavenClassLoaderConfigurationKey key = new MavenClassLoaderConfigurationKey(mavenArtifacts, Collections.<MavenClassLoaderConfigurationKey> emptyList(), scope,
                    moduleConfiguration);
            String name;
            if (scope == Scope.PLATFORM) {
                name = key.getArtifacts().toString();
            } else {
                name = appName;
            }
            classLoaderConfiguration = new ClassLoaderConfiguration(key, name, scope == Scope.ATTACHMENT, urls, Collections.<ClassLoaderConfiguration> emptyList(), null, null,
                    null, key.getModuleConfiguration());
            break;
        case FIXED_DEPENDENCIES:
            Map<MavenArtifact, URL> urlByKey = new HashMap<MavenArtifact, URL>();
            for (Artifact artifact : artifacts) {
                Map<String, MavenArtifact> map = runtimeDependencies.get(artifact.getGroupId());
                if (map == null) {
                    map = new HashMap<String, MavenArtifact>();
                    runtimeDependencies.put(artifact.getGroupId(), map);
                }
                MavenArtifact mavenArtifact = new MavenArtifact(artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion());
                map.put(artifact.getArtifactId(), mavenArtifact);
                urlByKey.put(mavenArtifact, artifact.getFile().toURI().toURL());
            }

            ClassLoaderConfigurationGraphHelper classLoaderConfigurationGraphHelper = new ClassLoaderConfigurationGraphHelper(appName, urlByKey, descriptorReader, collectRequest,
                    session, dependencyModifier, jpmsConfiguration, runtimeDependencies, scope, scopeModifier);

            GraphCycleRemover<NodeAndState, MavenArtifact, ClassLoaderConfigurationFactory> graphCycleRemover = new GraphCycleRemover<NodeAndState, MavenArtifact, ClassLoaderConfigurationFactory>(
                    classLoaderConfigurationGraphHelper);
            classLoaderConfiguration = graphCycleRemover.removeCycle(new NodeAndState(null, node, session.getDependencyManager())).create(stringParserFactory);
            break;
        default:
            throw new Exception("Unsupported resolve mode " + resolveMode);
        }
        LOGGER.info("Classloader configuration created");
        return classLoaderConfiguration;
    }

}
