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

import java.io.File;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.net.URL;
import java.util.List;
import java.util.Set;

/**
 * @author Gael Lalire
 */
public class ApplicationContext implements Serializable, Cloneable {

    private static final long serialVersionUID = -63902830158746259L;

    private AttachmentContext<RuntimeApplicationContext> launcherAttachmentContext;

    private AttachmentContext<RuntimeApplicationInstallerContext> installerAttachmentContext;

    private String name;

    private File config;

    private File data;

    private File cache;

    private int autoMigrateLevel;

    private Set<List<Integer>> supportedMigrationVersion;

    private Set<List<Integer>> uninterruptedMigrationVersion;

    private URL repoURL;

    private boolean autoStarted;

    private boolean uncommitted;

    private transient boolean locked;

    private RepositoryOverride repositoryOverride;

    private boolean trusted;

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(final boolean locked) {
        this.locked = locked;
    }

    public boolean isUncommitted() {
        return uncommitted;
    }

    public void setUncommitted(final boolean uncommitted) {
        this.uncommitted = uncommitted;
    }

    public boolean isAutoStarted() {
        return autoStarted;
    }

    public void setAutoStarted(final boolean autoStarted) {
        this.autoStarted = autoStarted;
    }

    public URL getRepoURL() {
        return repoURL;
    }

    public void setRepoURL(final URL repoURL) {
        this.repoURL = repoURL;
    }

    public String getRepoApplicationName() {
        return repoApplicationName;
    }

    public void setRepoApplicationName(final String repoApplicationName) {
        this.repoApplicationName = repoApplicationName;
    }

    private ApplicationContext migrationApplicationContext;

    public void setMigrationApplicationContext(final ApplicationContext migrationApplicationContext) {
        this.migrationApplicationContext = migrationApplicationContext;
    }

    public ApplicationContext getMigrationApplicationContext() {
        return migrationApplicationContext;
    }

    public List<Integer> getRepoApplicationVersion() {
        return repoApplicationVersion;
    }

    public void setRepoApplicationVersion(final List<Integer> repoApplicationVersion) {
        this.repoApplicationVersion = repoApplicationVersion;
    }

    private String repoApplicationName;

    private List<Integer> repoApplicationVersion;

    private transient VestigeSecureExecution<Void> vestigeSecureExecution;

    private transient boolean started;

    private transient String exceptionStackTrace;

    public boolean isStarted() {
        return started;
    }

    public void setStarted(final boolean started) {
        this.started = started;
    }

    public String getExceptionStackTrace() {
        return exceptionStackTrace;
    }

    public void setException(final Exception exception) {
        if (exception != null) {
            StringWriter stringWriter = new StringWriter();
            exception.printStackTrace(new PrintWriter(stringWriter));
            this.exceptionStackTrace = stringWriter.toString();
        } else {
            this.exceptionStackTrace = null;
        }
    }

    public File getConfig() {
        return config;
    }

    public void setConfig(final File config) {
        this.config = config;
    }

    public VestigeSecureExecution<Void> getVestigeSecureExecution() {
        return vestigeSecureExecution;
    }

    public void setVestigeSecureExecution(final VestigeSecureExecution<Void> vestigeSecureExecution) {
        this.vestigeSecureExecution = vestigeSecureExecution;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public int getAutoMigrateLevel() {
        return autoMigrateLevel;
    }

    public void setAutoMigrateLevel(final int autoMigrateLevel) {
        this.autoMigrateLevel = autoMigrateLevel;
    }

    public Set<List<Integer>> getSupportedMigrationVersion() {
        return supportedMigrationVersion;
    }

    public void setSupportedMigrationVersion(final Set<List<Integer>> supportedMigrationVersion) {
        this.supportedMigrationVersion = supportedMigrationVersion;
    }

    public Set<List<Integer>> getUninterruptedMigrationVersion() {
        return uninterruptedMigrationVersion;
    }

    public void setUninterruptedMigrationVersion(final Set<List<Integer>> uninterruptedMigrationVersion) {
        this.uninterruptedMigrationVersion = uninterruptedMigrationVersion;
    }

    public File getData() {
        return data;
    }

    public void setData(final File data) {
        this.data = data;
    }

    public File getCache() {
        return cache;
    }

    public void setCache(final File cache) {
        this.cache = cache;
    }

    public RepositoryOverride getOverrideURL() {
        return repositoryOverride;
    }

    public void setOverrideURL(final RepositoryOverride repositoryOverride) {
        this.repositoryOverride = repositoryOverride;
    }

    public boolean isTrusted() {
        return trusted;
    }

    public void setTrusted(final boolean trusted) {
        this.trusted = trusted;
    }

    // private void writeObject(ObjectOutputStream out) throws IOException {
    // out.defaultWriteObject();
    // resolve.save(out);
    // installerResolve.save(out);
    // }
    //
    // private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
    // in.defaultReadObject();
    // resolve = in.readObject();
    //
    // }

    public ApplicationContext copy() {
        try {
            ApplicationContext clone = (ApplicationContext) clone();
            clone.launcherAttachmentContext = clone.launcherAttachmentContext.copy();
            clone.installerAttachmentContext = clone.installerAttachmentContext.copy();
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    public AttachmentContext<RuntimeApplicationContext> getLauncherAttachmentContext() {
        return launcherAttachmentContext;
    }

    public void setLauncherAttachmentContext(final AttachmentContext<RuntimeApplicationContext> launcherAttachmentContext) {
        this.launcherAttachmentContext = launcherAttachmentContext;
    }

    public AttachmentContext<RuntimeApplicationInstallerContext> getInstallerAttachmentContext() {
        return installerAttachmentContext;
    }

    public void setInstallerAttachmentContext(final AttachmentContext<RuntimeApplicationInstallerContext> installerAttachmentContext) {
        this.installerAttachmentContext = installerAttachmentContext;
    }

    public static ApplicationResolvedClassLoaderConfiguration getResolve(final AttachmentContext<?> attachmentContext) {
        if (attachmentContext == null) {
            return null;
        }
        return attachmentContext.getResolve();
    }

    public static void setResolve(final AttachmentContext<?> attachmentContext, final ApplicationResolvedClassLoaderConfiguration resolve) {
        if (attachmentContext != null) {
            attachmentContext.setResolve(resolve);
        }
    }

    public static <RuntimeContext> void setRuntimeApplicationContext(final AttachmentContext<RuntimeContext> attachmentContext, final RuntimeContext context) {
        if (attachmentContext != null) {
            attachmentContext.setRuntimeApplicationContext(context);
        }
    }
}
