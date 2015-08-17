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

import java.net.URL;
import java.util.List;
import java.util.Set;

import fr.gaellalire.vestige.application.manager.ApplicationDescriptor;
import fr.gaellalire.vestige.application.manager.ApplicationDescriptorFactory;
import fr.gaellalire.vestige.application.manager.ApplicationException;

/**
 * @author Gael Lalire
 */
public class PropertiesApplicationDescriptorFactory implements ApplicationDescriptorFactory {

    public ApplicationDescriptor createApplicationDescriptor(final URL context, final String repoName, final String appName, final List<Integer> version)
            throws ApplicationException {
        return new PropertiesApplicationDescriptor();
    }

    @Override
    public boolean hasApplicationDescriptor(final URL context, final String repoName, final String appName, final List<Integer> version) throws ApplicationException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Set<String> listApplicationsName(final URL context) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Set<List<Integer>> listApplicationVersions(final URL context, final String appName) {
        // TODO Auto-generated method stub
        return null;
    }

}
