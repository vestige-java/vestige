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
public class ApplicationVerificationMetadata {

    private String launcherVerificationMetadata;

    private String installerVerificationMetadata;

    public ApplicationVerificationMetadata(final String launcherVerificationMetadata, final String installerVerificationMetadata) {
        this.launcherVerificationMetadata = launcherVerificationMetadata;
        this.installerVerificationMetadata = installerVerificationMetadata;
    }

    public void printLauncherSignature(final PrintWriter stringBuilder) {
    }

    public void printInstallerSignature(final PrintWriter stringBuilder) {
    }

    @Override
    public String toString() {
        StringWriter out = new StringWriter();
        PrintWriter printWriter = new PrintWriter(out);
        printWriter.println("-- launcher verification metadata --");
        printWriter.println(launcherVerificationMetadata);
        printLauncherSignature(printWriter);

        printWriter.println("-- installer verification metadata --");
        printWriter.println(installerVerificationMetadata);
        printInstallerSignature(printWriter);
        return out.toString();
    }

}
