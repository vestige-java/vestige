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

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fr.gaellalire.vestige.application.descriptor.xml.schema.application.Installer;
import fr.gaellalire.vestige.application.manager.ApplicationException;
import fr.gaellalire.vestige.application.manager.InstallerAttachmentDescriptor;
import fr.gaellalire.vestige.application.manager.PermissionSetFactory;
import fr.gaellalire.vestige.application.manager.VersionUtils;
import fr.gaellalire.vestige.spi.job.JobHelper;
import fr.gaellalire.vestige.spi.resolver.maven.MavenContext;
import fr.gaellalire.vestige.utils.SimpleValueGetter;

/**
 * @author Gael Lalire
 */
public class XMLInstallerAttachmentDescriptor extends XMLAttachmentDescriptor implements InstallerAttachmentDescriptor {

    private static final Pattern VERSION_RANGE_PATTERN = Pattern.compile("(\\d+)\\.(\\d+)\\.(\\d+)-(\\d+)");

    private List<Integer> version;

    private Installer installer;

    public XMLInstallerAttachmentDescriptor(final Installer installer, final XMLApplicationRepositoryManager xmlApplicationRepositoryManager, final JobHelper jobHelper,
            final MavenContext mavenContext, final PermissionSetFactory permissions) {
        super(installer, xmlApplicationRepositoryManager, jobHelper, mavenContext, permissions);
        this.installer = installer;
    }

    @Override
    public String getClassName() throws ApplicationException {
        return SimpleValueGetter.INSTANCE.getValue(installer.getClazz());
    }

    @Override
    public boolean isPrivateSystem() throws ApplicationException {
        return SimpleValueGetter.INSTANCE.getValue(installer.getPrivateSystem());
    }

    public Set<List<Integer>> getSupportedMigrationVersions() throws ApplicationException {
        Set<List<Integer>> supportedMigrationVersion = new HashSet<List<Integer>>();
        List<String> supportedMigration = installer.getSupportedMigration();
        if (supportedMigration != null) {
            for (String string : supportedMigration) {
                Matcher matcher = VERSION_RANGE_PATTERN.matcher(string);
                if (!matcher.matches()) {
                    throw new ApplicationException("illegal range");
                }
                int g = 1;
                int major = Integer.parseInt(matcher.group(g++));
                int minor = Integer.parseInt(matcher.group(g++));
                int firstBugfix = Integer.parseInt(matcher.group(g++));
                int lastBugfix = Integer.parseInt(matcher.group(g++));
                for (int i = firstBugfix; i <= lastBugfix; i++) {
                    List<Integer> otherVersion = Arrays.asList(major, minor, i);
                    if (version != null) {
                        Integer compare = VersionUtils.compare(version, otherVersion);
                        if (compare != null && compare.intValue() < 0) {
                            throw new ApplicationException("Version " + VersionUtils.toString(otherVersion) + " cannot be supported by " + VersionUtils.toString(version));
                        }
                    }
                    supportedMigrationVersion.add(otherVersion);
                }
            }
        }
        return supportedMigrationVersion;
    }

    public Set<List<Integer>> getUninterruptedMigrationVersions() throws ApplicationException {
        Set<List<Integer>> uninterruptedMigrationVersion = new HashSet<List<Integer>>();
        List<String> uninterruptedMigration = installer.getUninterruptedMigration();
        if (uninterruptedMigration != null) {
            for (String string : uninterruptedMigration) {
                Matcher matcher = VERSION_RANGE_PATTERN.matcher(string);
                if (!matcher.matches()) {
                    throw new ApplicationException("illegal range");
                }
                int g = 1;
                int major = Integer.parseInt(matcher.group(g++));
                int minor = Integer.parseInt(matcher.group(g++));
                int firstBugfix = Integer.parseInt(matcher.group(g++));
                int lastBugfix = Integer.parseInt(matcher.group(g++));
                for (int i = firstBugfix; i <= lastBugfix; i++) {
                    List<Integer> otherVersion = Arrays.asList(major, minor, i);
                    if (version != null) {
                        Integer compare = VersionUtils.compare(version, otherVersion);
                        if (compare != null && compare.intValue() < 0) {
                            throw new ApplicationException("Version " + VersionUtils.toString(otherVersion) + " cannot be supported by " + VersionUtils.toString(version));
                        }
                    }
                    uninterruptedMigrationVersion.add(otherVersion);
                }
            }
        }
        return uninterruptedMigrationVersion;
    }

}
