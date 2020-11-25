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

package fr.gaellalire.vestige.admin.ssh;

import java.io.IOException;

import org.apache.sshd.common.PtyMode;
import org.apache.sshd.common.util.Buffer;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.channel.ChannelSession;

/**
 * @author Gael Lalire
 */
public class VestigeChannelSession extends ChannelSession {

    @Override
    protected boolean handlePtyReq(final Buffer buffer) throws IOException {
        String term = buffer.getString();
        int tColumns = buffer.getInt();
        int tRows = buffer.getInt();
        int tWidth = buffer.getInt();
        int tHeight = buffer.getInt();
        byte[] modes = buffer.getBytes();
        for (int i = 0; i < modes.length && modes[i] != 0;) {
            PtyMode mode = PtyMode.fromInt(modes[i++]);
            if (mode != null) {
                int val = ((modes[i++] << 24) & 0xff000000) | ((modes[i++] << 16) & 0x00ff0000) | ((modes[i++] << 8) & 0x0000ff00) | ((modes[i++]) & 0x000000ff);
                getEnvironment().getPtyModes().put(mode, val);
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("pty for channel {}: term={}, size=({} - {}), pixels=({}, {}), modes=[{}]",
                    new Object[] {id, term, tColumns, tRows, tWidth, tHeight, getEnvironment().getPtyModes()});
        }
        addEnvVariable(Environment.ENV_TERM, term);
        addEnvVariable(Environment.ENV_COLUMNS, Integer.toString(tColumns));
        addEnvVariable(Environment.ENV_LINES, Integer.toString(tRows));
        return true;
    }

}
