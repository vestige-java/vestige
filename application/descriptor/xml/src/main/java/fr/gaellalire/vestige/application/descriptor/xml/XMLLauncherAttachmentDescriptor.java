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

import fr.gaellalire.vestige.application.descriptor.xml.schema.application.Launcher;
import fr.gaellalire.vestige.application.manager.ApplicationException;
import fr.gaellalire.vestige.application.manager.LauncherAttachmentDescriptor;
import fr.gaellalire.vestige.application.manager.PermissionSetFactory;
import fr.gaellalire.vestige.spi.job.JobHelper;
import fr.gaellalire.vestige.spi.resolver.maven.MavenContext;
import fr.gaellalire.vestige.utils.SimpleValueGetter;

/**
 * @author Gael Lalire
 */
public class XMLLauncherAttachmentDescriptor extends XMLAttachmentDescriptor implements LauncherAttachmentDescriptor {

    private Launcher launcher;

    public XMLLauncherAttachmentDescriptor(final Launcher launcher, final XMLApplicationRepositoryManager xmlApplicationRepositoryManager, final JobHelper jobHelper,
            final MavenContext mavenContext, final PermissionSetFactory permissions) {
        super(launcher, xmlApplicationRepositoryManager, jobHelper, mavenContext, permissions);
        this.launcher = launcher;
    }

    @Override
    public String getClassName() throws ApplicationException {
        return SimpleValueGetter.INSTANCE.getValue(launcher.getClazz());
    }

    @Override
    public boolean isPrivateSystem() throws ApplicationException {
        return SimpleValueGetter.INSTANCE.getValue(launcher.getPrivateSystem());
    }

}
