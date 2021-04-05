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
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.regex.Matcher;

/**
 * @author Gael Lalire
 */
public class WithHandlerURLFactory implements URLFactory {

    private URLStreamHandler urlStreamHandler;

    public WithHandlerURLFactory(final File baseDir) {
        urlStreamHandler = new URLStreamHandler() {

            @Override
            protected URLConnection openConnection(final URL u) throws IOException {
                Matcher matcher = MavenArtifactResolver.MVN_URL_PATTERN.matcher(u.toExternalForm());
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
    }

    @Override
    public URL createURL(final String spec) throws MalformedURLException {
        return new URL(null, spec, urlStreamHandler);
    }

}
