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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.aether.transfer.AbstractTransferListener;
import org.eclipse.aether.transfer.TransferCancelledException;
import org.eclipse.aether.transfer.TransferEvent;
import org.eclipse.aether.transfer.TransferResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.gaellalire.vestige.job.JobHelper;
import fr.gaellalire.vestige.job.TaskHelper;

/**
 * @author Gael Lalire
 */
public class VestigeTransferListener extends AbstractTransferListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(VestigeTransferListener.class);

    private JobHelper actionHelper;

    public VestigeTransferListener(final JobHelper actionHelper) {
        this.actionHelper = actionHelper;
    }

    private Map<TransferResource, TaskHelper> taskByResource = new HashMap<TransferResource, TaskHelper>();

    @Override
    public void transferInitiated(final TransferEvent event) throws TransferCancelledException {
        TransferResource resource = event.getResource();
        LOGGER.info("Transfering " + resource.getResourceName());
        TaskHelper addTask = actionHelper.addTask("Transfering " + resource.getResourceName());
        addTask.setProgress(0);
        taskByResource.put(resource, addTask);
    }

    @Override
    public void transferProgressed(final TransferEvent event) throws TransferCancelledException {
        TransferResource resource = event.getResource();
        long contentLength = resource.getContentLength();
        if (contentLength >= 0) {
            taskByResource.get(resource).setProgress(((float) event.getTransferredBytes()) / contentLength);
        }
    }

    @Override
    public void transferSucceeded(final TransferEvent event) {
        taskByResource.remove(event.getResource()).setDone();
    }

    @Override
    public void transferFailed(final TransferEvent event) {
        taskByResource.remove(event.getResource()).setDone();
    }

    @Override
    public void transferCorrupted(final TransferEvent event) throws TransferCancelledException {
        taskByResource.remove(event.getResource()).setDone();
    }

}
