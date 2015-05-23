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

package fr.gaellalire.vestige.resolver.maven;

import org.eclipse.aether.transfer.AbstractTransferListener;
import org.eclipse.aether.transfer.TransferCancelledException;
import org.eclipse.aether.transfer.TransferEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Gael Lalire
 */
public class VestigeTransferListener extends AbstractTransferListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(VestigeTransferListener.class);

    @Override
    public void transferInitiated(final TransferEvent event) throws TransferCancelledException {
        LOGGER.info("Transfering " + event.getResource().getResourceName());
    }

}
