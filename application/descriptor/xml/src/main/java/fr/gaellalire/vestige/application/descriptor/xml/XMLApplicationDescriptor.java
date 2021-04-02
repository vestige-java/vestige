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

import java.util.List;

import fr.gaellalire.vestige.application.descriptor.xml.schema.application.Application;
import fr.gaellalire.vestige.application.descriptor.xml.schema.application.Installer;
import fr.gaellalire.vestige.application.descriptor.xml.schema.application.Launcher;
import fr.gaellalire.vestige.application.manager.ApplicationDescriptor;
import fr.gaellalire.vestige.application.manager.ApplicationException;
import fr.gaellalire.vestige.application.manager.PermissionSetFactory;
import fr.gaellalire.vestige.spi.job.JobHelper;
import fr.gaellalire.vestige.spi.resolver.maven.MavenContext;

/**
 * @author Gael Lalire
 */
public class XMLApplicationDescriptor implements ApplicationDescriptor {

    private XMLApplicationRepositoryManager xmlApplicationRepositoryManager;

    private Application application;

    private MavenContext mavenContext;

    private MavenContext installerMavenContext;

    private PermissionSetFactory permissions;

    private PermissionSetFactory installerPermissions;

    private JobHelper jobHelper;

    private String javaSpecificationVersion;

    private String maxJavaSpecificationVersion;

    public XMLApplicationDescriptor(final XMLApplicationRepositoryManager xmlApplicationRepositoryManager, final String javaSpecificationVersion,
            final String maxJavaSpecificationVersion, final List<Integer> version, final Application application, final MavenContext mavenContext,
            final MavenContext installerMavenContext, final PermissionSetFactory permissions, final PermissionSetFactory installerPermissions, final JobHelper jobHelper) {
        this.xmlApplicationRepositoryManager = xmlApplicationRepositoryManager;
        this.javaSpecificationVersion = javaSpecificationVersion;
        this.maxJavaSpecificationVersion = maxJavaSpecificationVersion;
        this.application = application;
        this.mavenContext = mavenContext;
        this.installerMavenContext = installerMavenContext;
        this.permissions = permissions;
        this.installerPermissions = installerPermissions;
        this.jobHelper = jobHelper;
    }

    @Override
    public String getJavaSpecificationVersion() throws ApplicationException {
        return javaSpecificationVersion;
    }

    public String getMaxJavaSpecificationVersion() throws ApplicationException {
        return maxJavaSpecificationVersion;
    }

    @Override
    public XMLLauncherAttachmentDescriptor getLauncherAttachmentDescriptor() throws ApplicationException {
        Launcher launcher = application.getLauncher();
        if (launcher == null) {
            return null;
        }
        return new XMLLauncherAttachmentDescriptor(launcher, xmlApplicationRepositoryManager, jobHelper, mavenContext, permissions);
    }

    @Override
    public XMLInstallerAttachmentDescriptor getInstallerAttachmentDescriptor() throws ApplicationException {
        Installer installer = application.getInstaller();
        if (installer == null) {
            return null;
        }
        return new XMLInstallerAttachmentDescriptor(installer, xmlApplicationRepositoryManager, jobHelper, installerMavenContext, installerPermissions);
    }

}
