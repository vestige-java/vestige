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

package fr.gaellalire.vestige.platform.test;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import fr.gaellalire.vestige.platform.AttachmentVerificationMetadata;

/**
 * @author Gael Lalire
 */
public class TestAttachmentVerificationMetadata {

    public static void main(final String[] args) throws Exception {
        StringBuilder stringBuilder = new StringBuilder();
        BufferedReader br = new BufferedReader(new InputStreamReader(TestAttachmentVerificationMetadata.class.getResourceAsStream("/d.txt"), "UTF-8"));
        String readLine = br.readLine();
        while (readLine != null) {
            stringBuilder.append(readLine);
            stringBuilder.append('\n');
            readLine = br.readLine();
        }
        String metadata = stringBuilder.toString();
        System.out.println(metadata);
        AttachmentVerificationMetadata attachmentVerificationMetadata = AttachmentVerificationMetadata.fromString(metadata);
        String toString = attachmentVerificationMetadata.toString() + "\n";
        System.out.println(toString.equals(metadata));
        System.out.println(toString);
    }

}
