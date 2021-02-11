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
import java.io.StringWriter;

/**
 * @author Gael Lalire
 */
public class ApplicationVerificationMetadataSigned {

    private String launcherVerificationMetadata;

    private String launcherBase64Signature;

    private String installerVerificationMetadata;

    private String installerBase64Signature;

    public ApplicationVerificationMetadataSigned(final String launcherVerificationMetadata, final String launcherBase64Signature, final String installerVerificationMetadata,
            final String installerBase64Signature) {
        this.launcherVerificationMetadata = launcherVerificationMetadata;
        this.launcherBase64Signature = launcherBase64Signature;
        this.installerVerificationMetadata = installerVerificationMetadata;
        this.installerBase64Signature = installerBase64Signature;
    }

    @Override
    public String toString() {
        StringWriter out = new StringWriter();
        PrintWriter stringBuilder = new PrintWriter(out);
        stringBuilder.println("-- launcher verification metadata --");
        stringBuilder.println(launcherVerificationMetadata);
        stringBuilder.println("-- launcher pgp base64 signature  --");
        stringBuilder.println(launcherBase64Signature);

        stringBuilder.println("-- installer verification metadata --");
        stringBuilder.println(installerVerificationMetadata);
        stringBuilder.println("-- installer pgp base64 signature  --");
        stringBuilder.println(installerBase64Signature);
        return out.toString();
    }

}
