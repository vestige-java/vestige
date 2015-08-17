package fr.gaellalire.vestige.application.manager;

import java.util.List;

/**
 * Concrete class should provide at least one constructor whose parameters are : <ul>
 * <li>()</li>
 * <li>(File base)</li>
 * <li>(File base, File data)</li>
 * </ul>.
 *
 * @author Gael Lalire
 */
public interface ApplicationInstaller {

    void install() throws Exception;

    void uninstall() throws Exception;

    /**
     * Migrate from an older version to this version Override this method. You
     * should upgrade files in directories instead of removing all files and
     * install current version files.
     */
    void migrateFrom(List<Integer> fromVersion) throws Exception;

    /**
     * Migrate from this version to an older version Override this method. You
     * should downgrade files in directories instead of removing current version
     * files.
     */
    void migrateTo(List<Integer> toVersion) throws Exception;

    /**
     * Migrate from an older version to this version without stopping the
     * application. When this method is called fromRunnable is still running and the run method of runnable is not yet called.
     * When you call unlockThread.run() the runnable.run() will be called.
     */
    void uninterruptedMigrateFrom(List<Integer> fromVersion, Object fromRunnable, Object runnable, Runnable unlockThread) throws Exception;

    /**
     * Migrate from this version to an older version without stopping the
     * application. When this method is called fromRunnable is still running and the run method of runnable is not yet called.
     * When you call unlockToThread.run() the toRunnable.run() will be called.
     */
    void uninterruptedMigrateTo(Object runnable, List<Integer> toVersion, Object toRunnable, Runnable unlockToThread) throws Exception;
}
