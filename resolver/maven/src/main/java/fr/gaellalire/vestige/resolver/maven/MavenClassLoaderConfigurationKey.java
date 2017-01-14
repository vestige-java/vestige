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

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;

/**
 * @author Gael Lalire
 */
public class MavenClassLoaderConfigurationKey implements Serializable {

    private static final long serialVersionUID = 12014007030441749L;

    private List<MavenArtifact> artifacts;

    private List<MavenClassLoaderConfigurationKey> dependencies;

    private int hashCode;

    private Scope scope;

    public MavenClassLoaderConfigurationKey(final List<MavenArtifact> artifacts, final List<MavenClassLoaderConfigurationKey> dependencies, final Scope scope) {
        this.artifacts = artifacts;
        this.dependencies = dependencies;
        hashCode = artifacts.hashCode() + dependencies.hashCode();
        this.scope = scope;
    }

    public List<MavenArtifact> getArtifacts() {
        return artifacts;
    }

    public List<MavenClassLoaderConfigurationKey> getDependencies() {
        return dependencies;
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        toString(builder, 1);
        return builder.toString();
    }

    public void toString(final StringBuilder builder, final int indent) {
        builder.append(artifacts.toString());
        switch (scope) {
        case ATTACHMENT:
            builder.append(" attachment scoped");
            break;
        case INSTALLATION:
            builder.append(" installation scoped");
            break;
        default:
            break;
        }
        Iterator<MavenClassLoaderConfigurationKey> iterator = dependencies.iterator();
        while (iterator.hasNext()) {
            builder.append("\n");
            for (int i = 0; i < indent; i++) {
                builder.append("  ");
            }
            iterator.next().toString(builder, indent + 1);
        }
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (scope != Scope.PLATFORM) {
            return false;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof MavenClassLoaderConfigurationKey)) {
            return false;
        }
        MavenClassLoaderConfigurationKey other = (MavenClassLoaderConfigurationKey) obj;
        if (other.scope != Scope.PLATFORM) {
            return false;
        }
        if (!artifacts.equals(other.artifacts)) {
            return false;
        }
        if (!dependencies.equals(other.dependencies)) {
            return false;
        }
        return true;
    }

}
