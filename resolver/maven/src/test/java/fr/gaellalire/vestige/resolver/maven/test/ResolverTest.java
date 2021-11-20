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

package fr.gaellalire.vestige.resolver.maven.test;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;

import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import fr.gaellalire.vestige.core.VestigeCoreContext;
import fr.gaellalire.vestige.core.executor.VestigeWorker;
import fr.gaellalire.vestige.core.url.DelegateURLStreamHandlerFactory;
import fr.gaellalire.vestige.platform.ClassLoaderConfiguration;
import fr.gaellalire.vestige.platform.VestigeURLStreamHandlerFactory;
import fr.gaellalire.vestige.resolver.maven.CreateClassLoaderConfigurationParameters;
import fr.gaellalire.vestige.resolver.maven.DefaultDependencyModifier;
import fr.gaellalire.vestige.resolver.maven.DefaultMavenArtifact;
import fr.gaellalire.vestige.resolver.maven.DefaultResolvedMavenArtifact;
import fr.gaellalire.vestige.resolver.maven.MavenArtifactKey;
import fr.gaellalire.vestige.resolver.maven.MavenArtifactResolver;
import fr.gaellalire.vestige.resolver.maven.MavenRepository;
import fr.gaellalire.vestige.resolver.maven.ResolveParameters;
import fr.gaellalire.vestige.spi.job.DummyJobHelper;
import fr.gaellalire.vestige.spi.resolver.ResolvedClassLoaderConfiguration;
import fr.gaellalire.vestige.spi.resolver.Scope;
import fr.gaellalire.vestige.spi.resolver.maven.ResolveMode;
import fr.gaellalire.vestige.spi.resolver.maven.ResolvedMavenArtifact;

/**
 * @author Gael Lalire
 */
public class ResolverTest {

    @BeforeClass
    public static void init() throws InterruptedException {
        VestigeCoreContext vestigeCoreContext = VestigeCoreContext.buildDefaultInstance();
        final DelegateURLStreamHandlerFactory streamHandlerFactory = vestigeCoreContext.getStreamHandlerFactory();
        URL.setURLStreamHandlerFactory(streamHandlerFactory);

        VestigeURLStreamHandlerFactory vestigeURLStreamHandlerFactory = new VestigeURLStreamHandlerFactory();
        File baseDir = new File(System.getProperty("user.home"), ".m2" + File.separator + "repository");
        MavenArtifactResolver.replaceMavenURLStreamHandler(baseDir, vestigeURLStreamHandlerFactory);
        streamHandlerFactory.setDelegate(vestigeURLStreamHandlerFactory);
    }

    @Test
    @Ignore
    public void test() throws Exception {

        File settingsFile = new File(System.getProperty("user.home"), ".m2" + File.separator + "settings.xml");
        MavenArtifactResolver mavenArtifactResolver = new MavenArtifactResolver(null, new VestigeWorker[] {null}, settingsFile, null);

        ResolveParameters resolveRequest = new ResolveParameters();
        resolveRequest.setGroupId("fr.gaellalire.vestige_app.mywar");
        resolveRequest.setArtifactId("mywar");
        resolveRequest.setVersion("0.0.1");
        resolveRequest.setExtension("war");
        // resolveRequest.setAdditionalRepositories(additionalRepositories);
        DefaultDependencyModifier defaultDependencyModifier = new DefaultDependencyModifier();
        // defaultDependencyModifier.add("commons-io", "commons-io",
        // Collections.singletonList(new Dependency(new DefaultArtifact("fr.gaellalire.vestige_app.mywar", "mywar", "war", "0.0.1"), "runtime")));
        resolveRequest.setDependencyModifier(defaultDependencyModifier);
        // resolveRequest.setJpmsConfiguration(defaultJPMSConfiguration);
        // resolveRequest.setJpmsNamedModulesConfiguration(namedModulesConfiguration);
        resolveRequest.setSuperPomRepositoriesIgnored(true);
        resolveRequest.setPomRepositoriesIgnored(true);
        resolveRequest.setChecksumVerified(false);

        CreateClassLoaderConfigurationParameters createClassLoaderConfigurationParameters = new CreateClassLoaderConfigurationParameters();
        createClassLoaderConfigurationParameters.setManyLoaders(true);
        createClassLoaderConfigurationParameters.setAppName("mywar");
        createClassLoaderConfigurationParameters.setScope(Scope.PLATFORM);
        createClassLoaderConfigurationParameters.setSelfExcluded(true);

        if (true) {
            createClassLoaderConfigurationParameters
                    .setExcludesWithParents(new HashSet<MavenArtifactKey>(Arrays.asList(new MavenArtifactKey("javax.servlet", "javax.servlet-api", "jar", ""))));
        }

        ClassLoaderConfiguration resolve = mavenArtifactResolver.resolve(resolveRequest, DummyJobHelper.INSTANCE)
                .createClassLoaderConfiguration(createClassLoaderConfigurationParameters, new ArrayList<DefaultMavenArtifact>(), DummyJobHelper.INSTANCE);
        System.out.println(resolve);

    }

    @Test
    @Ignore
    public void testWar() throws Exception {

        File settingsFile = new File(System.getProperty("user.home"), ".m2" + File.separator + "settings.xml");
        MavenArtifactResolver mavenArtifactResolver = new MavenArtifactResolver(null, new VestigeWorker[] {null}, settingsFile, null);

        ResolveParameters resolveRequest = new ResolveParameters();
        resolveRequest.setGroupId("fr.gaellalire.vestige_app.mywar");
        resolveRequest.setArtifactId("mywar");
        resolveRequest.setVersion("0.0.1");
        resolveRequest.setExtension("war");
        resolveRequest.setClassifier("exploded-assembly");
        // resolveRequest.setAdditionalRepositories(additionalRepositories);
        DefaultDependencyModifier defaultDependencyModifier = new DefaultDependencyModifier();
        // defaultDependencyModifier.add("commons-io", "commons-io",
        // Collections.singletonList(new Dependency(new DefaultArtifact("fr.gaellalire.vestige_app.mywar", "mywar", "war", "0.0.1"), "runtime")));
        defaultDependencyModifier.add(new MavenArtifactKey("javax.servlet", "javax.servlet-api", "jar", ""),
                Collections.singletonList(new Dependency(new DefaultArtifact("fr.gaellalire.vestige_app.mywar", "mywar", "war", "0.0.1"), "runtime")));
        resolveRequest.setDependencyModifier(defaultDependencyModifier);
        // resolveRequest.setJpmsConfiguration(defaultJPMSConfiguration);
        // resolveRequest.setJpmsNamedModulesConfiguration(namedModulesConfiguration);
        resolveRequest.setSuperPomRepositoriesIgnored(true);
        resolveRequest.setPomRepositoriesIgnored(true);
        resolveRequest.setChecksumVerified(false);

        List<MavenRepository> additionalRepositories = new ArrayList<MavenRepository>();
        additionalRepositories.add(new MavenRepository("gaellalire-repo", null, "https://gaellalire.fr/maven/repository/"));
        resolveRequest.setAdditionalRepositories(additionalRepositories);

        CreateClassLoaderConfigurationParameters createClassLoaderConfigurationParameters = new CreateClassLoaderConfigurationParameters();
        createClassLoaderConfigurationParameters.setManyLoaders(true);
        createClassLoaderConfigurationParameters.setAppName("mywar");
        createClassLoaderConfigurationParameters.setScope(Scope.PLATFORM);
        // createClassLoaderConfigurationParameters.setSelfExcluded(true);

        ClassLoaderConfiguration resolve = mavenArtifactResolver.resolve(resolveRequest, DummyJobHelper.INSTANCE)
                .createClassLoaderConfiguration(createClassLoaderConfigurationParameters, new ArrayList<DefaultMavenArtifact>(), DummyJobHelper.INSTANCE);
        System.out.println(resolve);

    }

    @Test
    @Ignore
    public void testWarExcludeClasspath() throws Exception {

        File settingsFile = new File(System.getProperty("user.home"), ".m2" + File.separator + "settings.xml");
        MavenArtifactResolver mavenArtifactResolver = new MavenArtifactResolver(null, new VestigeWorker[] {null}, settingsFile, null);

        ResolveParameters resolveRequest = new ResolveParameters();
        resolveRequest.setGroupId("fr.gaellalire.vestige_app.mywar");
        resolveRequest.setArtifactId("mywar");
        resolveRequest.setVersion("0.0.1");
        resolveRequest.setExtension("war");
        // resolveRequest.setAdditionalRepositories(additionalRepositories);
        DefaultDependencyModifier defaultDependencyModifier = new DefaultDependencyModifier();
        // defaultDependencyModifier.add("commons-io", "commons-io",
        // Collections.singletonList(new Dependency(new DefaultArtifact("fr.gaellalire.vestige_app.mywar", "mywar", "war", "0.0.1"), "runtime")));
        // defaultDependencyModifier.add("javax.servlet", "javax.servlet-api",
        // Collections.singletonList(new Dependency(new DefaultArtifact("fr.gaellalire.vestige_app.mywar", "mywar", "war", "0.0.1"), "runtime")));
        resolveRequest.setDependencyModifier(defaultDependencyModifier);
        // resolveRequest.setJpmsConfiguration(defaultJPMSConfiguration);
        // resolveRequest.setJpmsNamedModulesConfiguration(namedModulesConfiguration);
        resolveRequest.setSuperPomRepositoriesIgnored(true);
        resolveRequest.setPomRepositoriesIgnored(true);
        resolveRequest.setChecksumVerified(false);

        CreateClassLoaderConfigurationParameters createClassLoaderConfigurationParameters = new CreateClassLoaderConfigurationParameters();
        createClassLoaderConfigurationParameters.setManyLoaders(false);
        createClassLoaderConfigurationParameters.setAppName("mywar");
        createClassLoaderConfigurationParameters.setScope(Scope.PLATFORM);
        HashSet<MavenArtifactKey> excludesWithParents = new HashSet<MavenArtifactKey>();
        excludesWithParents.add(new MavenArtifactKey("commons-io", "commons-io", "jar", ""));
        createClassLoaderConfigurationParameters.setExcludesWithParents(excludesWithParents);
        // createClassLoaderConfigurationParameters.setExcludes(excludesWithParents);

        ClassLoaderConfiguration resolve = mavenArtifactResolver.resolve(resolveRequest, DummyJobHelper.INSTANCE)
                .createClassLoaderConfiguration(createClassLoaderConfigurationParameters, new ArrayList<DefaultMavenArtifact>(), DummyJobHelper.INSTANCE);
        System.out.println(resolve);

    }

    @Test
    @Ignore
    public void testEAR() throws Exception {

        File settingsFile = new File(System.getProperty("user.home"), ".m2" + File.separator + "settings.xml");
        MavenArtifactResolver mavenArtifactResolver = new MavenArtifactResolver(null, new VestigeWorker[] {null}, settingsFile, null);

        ResolveParameters resolveRequest = new ResolveParameters();
        resolveRequest.setGroupId("fr.gaellalire.vestige_app.myeeapps");
        resolveRequest.setArtifactId("myeeapps-ear1");
        resolveRequest.setVersion("0.0.1");
        resolveRequest.setExtension("ear");
        // resolveRequest.setAdditionalRepositories(additionalRepositories);
        DefaultDependencyModifier defaultDependencyModifier = new DefaultDependencyModifier();
        // defaultDependencyModifier.add("commons-io", "commons-io",
        // Collections.singletonList(new Dependency(new DefaultArtifact("fr.gaellalire.vestige_app.mywar", "mywar", "war", "0.0.1"), "runtime")));
        defaultDependencyModifier.add(new MavenArtifactKey("javax.servlet", "javax.servlet-api", "jar", ""),
                Collections.singletonList(new Dependency(new DefaultArtifact("fr.gaellalire.vestige_app.mywar", "mywar", "war", "0.0.1"), "runtime")));
        resolveRequest.setDependencyModifier(defaultDependencyModifier);
        // resolveRequest.setJpmsConfiguration(defaultJPMSConfiguration);
        // resolveRequest.setJpmsNamedModulesConfiguration(namedModulesConfiguration);
        resolveRequest.setSuperPomRepositoriesIgnored(true);
        resolveRequest.setPomRepositoriesIgnored(true);
        resolveRequest.setChecksumVerified(false);

        CreateClassLoaderConfigurationParameters createClassLoaderConfigurationParameters = new CreateClassLoaderConfigurationParameters();
        createClassLoaderConfigurationParameters.setManyLoaders(true);
        createClassLoaderConfigurationParameters.setAppName("myear1");
        createClassLoaderConfigurationParameters.setScope(Scope.PLATFORM);
        createClassLoaderConfigurationParameters.setSelfExcluded(true);

        ClassLoaderConfiguration resolve = mavenArtifactResolver.resolve(resolveRequest, DummyJobHelper.INSTANCE)
                .createClassLoaderConfiguration(createClassLoaderConfigurationParameters, new ArrayList<DefaultMavenArtifact>(), DummyJobHelper.INSTANCE);
        System.out.println(resolve);

    }

    @Test
    @Ignore
    public void testClasspath() throws Exception {

        File settingsFile = new File(System.getProperty("user.home"), ".m2" + File.separator + "settings.xml");
        MavenArtifactResolver mavenArtifactResolver = new MavenArtifactResolver(null, null, settingsFile, null);

        ResolveParameters resolveRequest = new ResolveParameters();
        resolveRequest.setGroupId("fr.gaellalire.vestige_app.myeeapps");
        resolveRequest.setArtifactId("myeeapps-ear1");
        resolveRequest.setVersion("0.0.1");
        resolveRequest.setExtension("ear");
        // resolveRequest.setAdditionalRepositories(additionalRepositories);
        DefaultDependencyModifier defaultDependencyModifier = new DefaultDependencyModifier();
        // defaultDependencyModifier.add("commons-io", "commons-io",
        // Collections.singletonList(new Dependency(new DefaultArtifact("fr.gaellalire.vestige_app.mywar", "mywar", "war", "0.0.1"), "runtime")));
        // defaultDependencyModifier.add("javax.servlet", "javax.servlet-api",
        // Collections.singletonList(new Dependency(new DefaultArtifact("fr.gaellalire.vestige_app.mywar", "mywar", "war", "0.0.1"), "runtime")));
        resolveRequest.setDependencyModifier(defaultDependencyModifier);
        // resolveRequest.setJpmsConfiguration(defaultJPMSConfiguration);
        // resolveRequest.setJpmsNamedModulesConfiguration(namedModulesConfiguration);
        resolveRequest.setSuperPomRepositoriesIgnored(true);
        resolveRequest.setPomRepositoriesIgnored(true);
        resolveRequest.setChecksumVerified(false);

        // CreateClassLoaderConfigurationParameters createClassLoaderConfigurationParameters = new CreateClassLoaderConfigurationParameters();
        // createClassLoaderConfigurationParameters.setManyLoaders(false);
        // createClassLoaderConfigurationParameters.setAppName("myear1");
        // createClassLoaderConfigurationParameters.setScope(Scope.PLATFORM);
        // createClassLoaderConfigurationParameters.setSelfExcluded(true);

        DefaultResolvedMavenArtifact artif = mavenArtifactResolver.resolve(resolveRequest, DummyJobHelper.INSTANCE);
        System.out.println(artif.createClassLoaderConfiguration("ear1", ResolveMode.CLASSPATH, Scope.PLATFORM).execute());
        System.out.println();

        Enumeration<? extends ResolvedMavenArtifact> dependencies = artif.getDependencies();
        ResolvedMavenArtifact war1 = dependencies.nextElement();

        System.out.println(war1.createClassLoaderConfiguration("war1", ResolveMode.CLASSPATH, Scope.PLATFORM).execute());

        resolveRequest.setGroupId(war1.getGroupId());
        resolveRequest.setArtifactId(war1.getArtifactId());
        resolveRequest.setVersion(war1.getVersion());
        resolveRequest.setExtension(war1.getExtension());

        DefaultResolvedMavenArtifact war1Direct = mavenArtifactResolver.resolve(resolveRequest, DummyJobHelper.INSTANCE);
        ResolvedClassLoaderConfiguration resolveDirect = war1Direct.createClassLoaderConfiguration("mywar1-direct", ResolveMode.CLASSPATH, Scope.PLATFORM).execute();
        System.out.println(resolveDirect);
        System.out.println();

        @SuppressWarnings("unused")
        ResolvedMavenArtifact ejb1 = dependencies.nextElement();
        ResolvedMavenArtifact ejb2 = dependencies.nextElement();
        ResolvedClassLoaderConfiguration resolve = ejb2.createClassLoaderConfiguration("myejb2", ResolveMode.CLASSPATH, Scope.PLATFORM).execute();
        System.out.println(resolve);

        resolveRequest.setGroupId(ejb2.getGroupId());
        resolveRequest.setArtifactId(ejb2.getArtifactId());
        resolveRequest.setVersion(ejb2.getVersion());
        resolveRequest.setExtension(ejb2.getExtension());

        DefaultResolvedMavenArtifact ejb2Direct = mavenArtifactResolver.resolve(resolveRequest, DummyJobHelper.INSTANCE);
        resolveDirect = ejb2Direct.createClassLoaderConfiguration("myejb2-direct", ResolveMode.CLASSPATH, Scope.PLATFORM).execute();
        System.out.println(resolveDirect);

    }

}
