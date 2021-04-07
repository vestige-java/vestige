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

package fr.gaellalire.vestige.edition.maven_main_launcher;

import java.io.Serializable;

import fr.gaellalire.vestige.platform.ClassLoaderConfiguration;

/**
 * @author Gael Lalire
 */
public class VerifiedClassLoaderConfiguration implements Serializable {

    private static final long serialVersionUID = -7520011331157690587L;

    private ClassLoaderConfiguration classLoaderConfiguration;

    private String verificationMetadata;

    public VerifiedClassLoaderConfiguration(final ClassLoaderConfiguration classLoaderConfiguration, final String verificationMetadata) {
        this.classLoaderConfiguration = classLoaderConfiguration;
        this.verificationMetadata = verificationMetadata;
    }

    public ClassLoaderConfiguration getClassLoaderConfiguration() {
        return classLoaderConfiguration;
    }

    public String getVerificationMetadata() {
        return verificationMetadata;
    }

    @Override
    public String toString() {
        return classLoaderConfiguration.toString();
    }

    public boolean verify() {
        return classLoaderConfiguration.verify();
    }

}
