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

    /**
     * @throws Exception if thrown the application will not be installed
     */
    void install() throws Exception;

    /**
     * Allow you to clean files which are not in base and data directories.
     * If you respect vestige template, this method should not be implemented.
     * @throws Exception does nothing
     */
    void uninstall() throws Exception;

    /**
     * Migrate from an older version to this version Override this method. You
     * should upgrade files in directories instead of removing all files and
     * install current version files.
     * If this method does not throw an exception and the server does not crash {@link #commitMigration()} will be called.
     * If the server crash either {@link #commitMigration()} or {@link #cleanMigration()} will be called at restart.
     * @throws Exception if thrown the {@link #cleanMigration()} will be called and the migration will be aborted
     */
    void prepareMigrateFrom(List<Integer> fromVersion) throws Exception;

    /**
     * Migrate from this version to an older version Override this method. You
     * should downgrade files in directories instead of removing current version
     * files.
     * If this method does not throw an exception and the server does not crash {@link #commitMigration()} will be called.
     * If the server crash either {@link #commitMigration()} or {@link #cleanMigration()} will be called at restart.
     * @throws Exception if thrown the {@link #cleanMigration()} will be called and the migration will be aborted
     */
    void prepareMigrateTo(List<Integer> toVersion) throws Exception;

    /**
     * Migrate from an older version to this version without stopping the
     * application. When this method is called fromRunnable is still running and the run method of runnable is not yet called.
     * When you call unlockThread.run() the runnable.run() will be called.
     * If this method does not throw an exception and the server does not crash {@link #commitMigration()} will be called.
     * If the server crash either {@link #commitMigration()} or {@link #cleanMigration()} will be called at restart.
     * @throws Exception if thrown the {@link #cleanMigration()} will be called and the migration will be aborted
     */
    void prepareUninterruptedMigrateFrom(List<Integer> fromVersion, Object fromRunnable, Object runnable, Runnable unlockThread) throws Exception;

    /**
     * Migrate from this version to an older version without stopping the
     * application. When this method is called fromRunnable is still running and the run method of runnable is not yet called.
     * When you call unlockToThread.run() the toRunnable.run() will be called.
     * If this method does not throw an exception and the server does not crash {@link #commitMigration()} will be called.
     * If the server crash either {@link #commitMigration()} or {@link #cleanMigration()} will be called at restart.
     * @throws Exception if thrown the {@link #cleanMigration()} will be called and the migration will be aborted
     */
    void prepareUninterruptedMigrateTo(Object runnable, List<Integer> toVersion, Object toRunnable, Runnable unlockToThread) throws Exception;

    /**
     * Will be called if one of {@link #prepareMigrateFrom(List)} {@link #prepareMigrateTo(List)} {@link #prepareUninterruptedMigrateFrom(List, Object, Object, Runnable)} {@link #prepareUninterruptedMigrateTo(Object, List, Object, Runnable)} throws an exception. <br>
     * This method will be called again if the server crash.
     * @throws Exception if thrown the application will be uninstalled but its base and data files will stay in your file system for manual recover.
     */
    void cleanMigration() throws Exception;

    /**
     * Will be called if one of {@link #prepareMigrateFrom(List)} {@link #prepareMigrateTo(List)} {@link #prepareUninterruptedMigrateFrom(List, Object, Object, Runnable)} {@link #prepareUninterruptedMigrateTo(Object, List, Object, Runnable)} returns
     * without exception. <br>
     * This method will be called again if the server crash.
     * @throws Exception if thrown the application will be uninstalled but its base and data files will stay in your file system for manual recover.
     */
    void commitMigration() throws Exception;

}
