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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import fr.gaellalire.vestige.platform.JPMSClassLoaderConfiguration;
import fr.gaellalire.vestige.platform.JPMSNamedModulesConfiguration;
import fr.gaellalire.vestige.spi.resolver.Scope;

/**
 * @author Gael Lalire
 */
public class MavenClassLoaderConfigurationKey implements Serializable {

    private static final long serialVersionUID = 12014007030441749L;

    private List<DefaultMavenArtifact> artifacts;

    private List<MavenClassLoaderConfigurationKey> dependencies;

    private JPMSClassLoaderConfiguration moduleConfiguration;

    private int hashCode;

    private Scope scope;

    private JPMSNamedModulesConfiguration namedModulesConfiguration;

    private boolean[] beforeParents;

    public MavenClassLoaderConfigurationKey(final List<DefaultMavenArtifact> artifacts, final List<MavenClassLoaderConfigurationKey> dependencies, final Scope scope,
            final JPMSClassLoaderConfiguration moduleConfiguration, final JPMSNamedModulesConfiguration namedModulesConfiguration, final boolean[] beforeParents) {
        this.artifacts = artifacts;
        this.dependencies = dependencies;
        hashCode = artifacts.hashCode() + dependencies.hashCode();
        this.scope = scope;
        this.moduleConfiguration = moduleConfiguration;
        this.namedModulesConfiguration = namedModulesConfiguration;
        this.beforeParents = beforeParents;
    }

    public JPMSClassLoaderConfiguration getModuleConfiguration() {
        return moduleConfiguration;
    }

    public List<DefaultMavenArtifact> getArtifacts() {
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
        List<MavenClassLoaderConfigurationKey> alreadyPrinted = new ArrayList<MavenClassLoaderConfigurationKey>();
        StringBuilder builder = new StringBuilder();
        toString(alreadyPrinted, builder, 1);
        return builder.toString();
    }

    public void toString(final List<MavenClassLoaderConfigurationKey> alreadyPrinted, final StringBuilder builder, final int indent) {
        int indexOf = alreadyPrinted.indexOf(this);
        if (indexOf != -1) {
            builder.append("@");
            builder.append(indexOf);
            return;
        }
        indexOf = alreadyPrinted.size();
        alreadyPrinted.add(this);
        builder.append(artifacts.toString());
        switch (scope) {
        case ATTACHMENT:
            builder.append(" attachment scoped");
            break;
        case CLASS_LOADER_CONFIGURATION:
            builder.append(" configuration scoped");
            break;
        default:
            break;
        }
        builder.append(" @");
        builder.append(indexOf);
        Iterator<MavenClassLoaderConfigurationKey> iterator = dependencies.iterator();
        while (iterator.hasNext()) {
            builder.append("\n");
            for (int i = 0; i < indent; i++) {
                builder.append("  ");
            }
            iterator.next().toString(alreadyPrinted, builder, indent + 1);
        }
    }

    public boolean[] getBeforeParents() {
        return beforeParents;
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
        if (moduleConfiguration == null) {
            if (other.moduleConfiguration != null) {
                return false;
            }
        } else if (!moduleConfiguration.equals(other.moduleConfiguration)) {
            return false;
        }
        if (namedModulesConfiguration == null) {
            if (other.namedModulesConfiguration != null) {
                return false;
            }
        } else if (!namedModulesConfiguration.equals(other.namedModulesConfiguration)) {
            return false;
        }

        if (beforeParents == null) {
            if (other.beforeParents != null) {
                for (int i = 0; i < other.beforeParents.length; i++) {
                    if (other.beforeParents[i]) {
                        return false;
                    }
                }
            }
        } else {
            if (other.beforeParents == null) {
                for (int i = 0; i < beforeParents.length; i++) {
                    if (beforeParents[i]) {
                        return false;
                    }
                }
            } else {
                if (beforeParents.length != other.beforeParents.length) {
                    return false;
                }
                for (int i = 0; i < beforeParents.length; i++) {
                    if (beforeParents[i] != other.beforeParents[i]) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

}
