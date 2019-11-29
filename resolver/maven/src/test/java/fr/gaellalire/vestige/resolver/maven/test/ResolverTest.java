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
import java.util.Collections;

import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.gaellalire.vestige.core.executor.VestigeExecutor;
import fr.gaellalire.vestige.core.url.DelegateURLStreamHandlerFactory;
import fr.gaellalire.vestige.platform.ClassLoaderConfiguration;
import fr.gaellalire.vestige.platform.DefaultVestigePlatform;
import fr.gaellalire.vestige.platform.VestigeURLStreamHandlerFactory;
import fr.gaellalire.vestige.resolver.maven.CreateClassLoaderConfigurationParameters;
import fr.gaellalire.vestige.resolver.maven.DefaultDependencyModifier;
import fr.gaellalire.vestige.resolver.maven.MavenArtifactResolver;
import fr.gaellalire.vestige.resolver.maven.ResolveParameters;
import fr.gaellalire.vestige.spi.job.DummyJobHelper;
import fr.gaellalire.vestige.spi.resolver.Scope;

/**
 * @author Gael Lalire
 */
public class ResolverTest {

    @BeforeClass
    public static void init() {
        DelegateURLStreamHandlerFactory streamHandlerFactory = new DelegateURLStreamHandlerFactory();
        URL.setURLStreamHandlerFactory(streamHandlerFactory);
        VestigeURLStreamHandlerFactory vestigeURLStreamHandlerFactory = new VestigeURLStreamHandlerFactory();
        File baseDir = new File(System.getProperty("user.home"), ".m2" + File.separator + "repository");
        MavenArtifactResolver.replaceMavenURLStreamHandler(baseDir, vestigeURLStreamHandlerFactory);
        streamHandlerFactory.setDelegate(vestigeURLStreamHandlerFactory);
    }

    @Test
    public void test() throws Exception {

        File settingsFile = new File(System.getProperty("user.home"), ".m2" + File.separator + "settings.xml");
        MavenArtifactResolver mavenArtifactResolver = new MavenArtifactResolver(new DefaultVestigePlatform(new VestigeExecutor(), null), settingsFile);

        ResolveParameters resolveRequest = new ResolveParameters();
        resolveRequest.setGroupId("fr.gaellalire.vestige_app.mywar");
        resolveRequest.setArtifactId("mywar");
        resolveRequest.setVersion("0.0.1");
        resolveRequest.setExtension("war");
        // resolveRequest.setAdditionalRepositories(additionalRepositories);
        DefaultDependencyModifier defaultDependencyModifier = new DefaultDependencyModifier();
        // defaultDependencyModifier.add("commons-io", "commons-io",
        // Collections.singletonList(new Dependency(new DefaultArtifact("fr.gaellalire.vestige_app.mywar", "mywar", "war", "0.0.1"), "runtime")));
        defaultDependencyModifier.add("javax.servlet", "javax.servlet-api",
                Collections.singletonList(new Dependency(new DefaultArtifact("fr.gaellalire.vestige_app.mywar", "mywar", "war", "0.0.1"), "runtime")));
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

        ClassLoaderConfiguration resolve = mavenArtifactResolver.resolve(resolveRequest, DummyJobHelper.INSTANCE)
                .createClassLoaderConfiguration(createClassLoaderConfigurationParameters);
        System.out.println(resolve);

    }

    @Test
    public void testEAR() throws Exception {

        File settingsFile = new File(System.getProperty("user.home"), ".m2" + File.separator + "settings.xml");
        MavenArtifactResolver mavenArtifactResolver = new MavenArtifactResolver(new DefaultVestigePlatform(new VestigeExecutor(), null), settingsFile);

        ResolveParameters resolveRequest = new ResolveParameters();
        resolveRequest.setGroupId("fr.gaellalire.vestige_app.myeeapps");
        resolveRequest.setArtifactId("myeeapps-ear1");
        resolveRequest.setVersion("0.0.1");
        resolveRequest.setExtension("ear");
        // resolveRequest.setAdditionalRepositories(additionalRepositories);
        DefaultDependencyModifier defaultDependencyModifier = new DefaultDependencyModifier();
        // defaultDependencyModifier.add("commons-io", "commons-io",
        // Collections.singletonList(new Dependency(new DefaultArtifact("fr.gaellalire.vestige_app.mywar", "mywar", "war", "0.0.1"), "runtime")));
        defaultDependencyModifier.add("javax.servlet", "javax.servlet-api",
                Collections.singletonList(new Dependency(new DefaultArtifact("fr.gaellalire.vestige_app.mywar", "mywar", "war", "0.0.1"), "runtime")));
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
        createClassLoaderConfigurationParameters.setSelfExcluded(true);

        ClassLoaderConfiguration resolve = mavenArtifactResolver.resolve(resolveRequest, DummyJobHelper.INSTANCE)
                .createClassLoaderConfiguration(createClassLoaderConfigurationParameters);
        System.out.println(resolve);

    }

}
