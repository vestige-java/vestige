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

package fr.gaellalire.vestige.application.descriptor.xml;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import fr.gaellalire.vestige.application.manager.ApplicationRepositoryMetadata;

/**
 * @author Gael Lalire
 */
public class XMLApplicationRepositoryMetadata implements ApplicationRepositoryMetadata {

    private static final Set<List<Integer>> EMPTY_VERSION_SET = Collections.emptySet();

    private Map<String, Set<List<Integer>>> versionsByNames = new TreeMap<String, Set<List<Integer>>>();

    public XMLApplicationRepositoryMetadata(final Map<String, Set<List<Integer>>> versionsByNames) {
        this.versionsByNames = versionsByNames;
    }

    public Set<String> listApplicationsName() {
        return versionsByNames.keySet();
    }

    public Set<List<Integer>> listApplicationVersions(final String appName) {
        Set<List<Integer>> set = versionsByNames.get(appName);
        if (set == null) {
            return EMPTY_VERSION_SET;
        }
        return set;
    }

}
