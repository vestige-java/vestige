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

package fr.gaellalire.vestige.application.manager;

import java.io.PrintWriter;

/**
 * @author Gael Lalire
 */
public class ApplicationVerificationMetadataPGPSigned extends ApplicationVerificationMetadata {

    private String launcherBase64Signature;

    private String installerBase64Signature;

    public ApplicationVerificationMetadataPGPSigned(final String launcherVerificationMetadata, final String launcherBase64Signature, final String installerVerificationMetadata,
            final String installerBase64Signature) {
        super(launcherVerificationMetadata, installerVerificationMetadata);
        this.launcherBase64Signature = launcherBase64Signature;
        this.installerBase64Signature = installerBase64Signature;
    }

    public void printLauncherSignature(final PrintWriter stringBuilder) {
        stringBuilder.println("-- launcher pgp base64 signature  --");
        stringBuilder.println(launcherBase64Signature);
    }

    public void printInstallerSignature(final PrintWriter stringBuilder) {
        stringBuilder.println("-- installer pgp base64 signature  --");
        stringBuilder.println(installerBase64Signature);
    }

}
