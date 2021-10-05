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

package fr.gaellalire.vestige.resolver.url_list;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import fr.gaellalire.vestige.core.executor.VestigeWorker;
import fr.gaellalire.vestige.platform.ClassLoaderConfiguration;
import fr.gaellalire.vestige.platform.FileWithMetadata;
import fr.gaellalire.vestige.platform.JPMSClassLoaderConfiguration;
import fr.gaellalire.vestige.platform.ModuleConfiguration;
import fr.gaellalire.vestige.platform.VestigePlatform;
import fr.gaellalire.vestige.resolver.common.DefaultResolvedClassLoaderConfiguration;
import fr.gaellalire.vestige.spi.job.JobHelper;
import fr.gaellalire.vestige.spi.resolver.ResolvedClassLoaderConfiguration;
import fr.gaellalire.vestige.spi.resolver.ResolverException;
import fr.gaellalire.vestige.spi.resolver.Scope;
import fr.gaellalire.vestige.spi.resolver.url_list.URLListRequest;
import fr.gaellalire.vestige.spi.resolver.url_list.VestigeURLListResolver;

/**
 * @author Gael Lalire
 */
public class DefaultVestigeURLListResolver implements VestigeURLListResolver {

    private VestigePlatform vestigePlatform;

    private VestigeWorker[] vestigeWorker;

    public DefaultVestigeURLListResolver(final VestigePlatform vestigePlatform, final VestigeWorker[] vestigeWorker) {
        this.vestigePlatform = vestigePlatform;
        this.vestigeWorker = vestigeWorker;
    }

    @Override
    public URLListRequest createURLListRequest(final Scope scope, final String configurationName) {

        return new URLListRequest() {

            private List<URL> urls = new ArrayList<URL>();

            private List<ModuleConfiguration> moduleConfigurations = new ArrayList<ModuleConfiguration>();

            @Override
            public ResolvedClassLoaderConfiguration execute(final JobHelper jobHelper) throws ResolverException {
                URLClassLoaderConfigurationKey key;
                String name;
                List<FileWithMetadata> files = new ArrayList<FileWithMetadata>();
                // FIXME copy url content in a dir
                for (URL url : urls) {
                    try {
                        // FIXME url permission ??
                        files.add(new FileWithMetadata(new File(url.toURI()), url, null, null));
                    } catch (URISyntaxException e) {
                        throw new ResolverException("Unable to get URL", e);
                    }
                }
                URL[] urlArray = urls.toArray(new URL[urls.size()]);
                if (scope == Scope.PLATFORM) {
                    key = new URLClassLoaderConfigurationKey(true, urlArray);
                    name = urls.toString();
                } else {
                    key = new URLClassLoaderConfigurationKey(false, urlArray);
                    name = configurationName;
                }
                return new DefaultResolvedClassLoaderConfiguration(vestigePlatform, vestigeWorker[0],
                        new ClassLoaderConfiguration(key, name, scope == Scope.ATTACHMENT, Collections.<FileWithMetadata> emptyList(), files,
                                Collections.<ClassLoaderConfiguration> emptyList(), null, null, null, null, JPMSClassLoaderConfiguration.EMPTY_INSTANCE.merge(moduleConfigurations),
                                null, false),
                        true);
            }

            public void addExports(final String moduleName, final String packageName) {
                moduleConfigurations.add(new ModuleConfiguration(moduleName, Collections.singleton(packageName), Collections.<String> emptySet(), null));
            }

            public void addOpens(final String moduleName, final String packageName) {
                moduleConfigurations.add(new ModuleConfiguration(moduleName, Collections.<String> emptySet(), Collections.singleton(packageName), null));
            }

            @Override
            public void addURL(final URL url) {
                urls.add(url);
            }
        };
    }

    @Override
    public ResolvedClassLoaderConfiguration restoreSavedResolvedClassLoaderConfiguration(final ObjectInputStream objectInputStream) throws IOException {
        int size = objectInputStream.readInt();
        byte[] array = new byte[size];
        objectInputStream.readFully(array);
        try {
            ClassLoaderConfiguration classLoaderConfiguration = (ClassLoaderConfiguration) new ObjectInputStream(new ByteArrayInputStream(array)) {

                protected Class<?> resolveClass(final ObjectStreamClass desc) throws IOException, ClassNotFoundException {
                    return Class.forName(desc.getName(), false, DefaultVestigeURLListResolver.class.getClassLoader());
                }

            }.readObject();
            return new DefaultResolvedClassLoaderConfiguration(vestigePlatform, vestigeWorker[0], classLoaderConfiguration, true);
        } catch (ClassNotFoundException e) {
            throw new IOException("ClassNotFoundException", e);
        }
    }

}
