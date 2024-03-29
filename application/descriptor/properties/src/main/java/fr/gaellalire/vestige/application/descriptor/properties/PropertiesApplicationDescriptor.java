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

package fr.gaellalire.vestige.application.descriptor.properties;

import fr.gaellalire.vestige.application.manager.ApplicationDescriptor;
import fr.gaellalire.vestige.application.manager.ApplicationException;
import fr.gaellalire.vestige.application.manager.InstallerAttachmentDescriptor;
import fr.gaellalire.vestige.application.manager.LauncherAttachmentDescriptor;

/**
 * @author Gael Lalire
 */
public class PropertiesApplicationDescriptor implements ApplicationDescriptor {

    @Override
    public String getJavaSpecificationVersion() throws ApplicationException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public LauncherAttachmentDescriptor getLauncherAttachmentDescriptor() throws ApplicationException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public InstallerAttachmentDescriptor getInstallerAttachmentDescriptor() throws ApplicationException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getMaxJavaSpecificationVersion() throws ApplicationException {
        // TODO Auto-generated method stub
        return null;
    }

}
