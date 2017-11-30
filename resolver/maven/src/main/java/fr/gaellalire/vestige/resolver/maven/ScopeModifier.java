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

import java.util.HashMap;
import java.util.Map;

import fr.gaellalire.vestige.spi.resolver.Scope;

/**
 * @author Gael Lalire
 */
public class ScopeModifier {

    private Map<String, Map<String, Scope>> scopeByArtifactIdByGroupId = new HashMap<String, Map<String, Scope>>();

    public void put(final String groupId, final String artifactId, final Scope scope) {
        Map<String, Scope> scopeByArtifactId = scopeByArtifactIdByGroupId.get(groupId);
        if (scopeByArtifactId == null) {
            scopeByArtifactId = new HashMap<String, Scope>();
            scopeByArtifactIdByGroupId.put(groupId, scopeByArtifactId);
        }
        scopeByArtifactId.put(artifactId, scope);
    }

    public Scope modify(final Scope scope, final String groupId, final String artifactId) {
        Map<String, Scope> scopeByArtifactId = scopeByArtifactIdByGroupId.get(groupId);
        if (scopeByArtifactId == null) {
            return scope;
        }
        Scope modifiedScope = scopeByArtifactId.get(artifactId);
        if (modifiedScope == null) {
            return scope;
        }
        return modifiedScope;
    }

}
