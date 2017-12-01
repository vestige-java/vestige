/// *
// * This file is part of Vestige.
// *
// * Vestige is free software: you can redistribute it and/or modify
// * it under the terms of the GNU General Public License as published by
// * the Free Software Foundation, either version 3 of the License, or
// * (at your option) any later version.
// *
// * Vestige is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// * GNU General Public License for more details.
// *
// * You should have received a copy of the GNU General Public License
// * along with Vestige. If not, see <http://www.gnu.org/licenses/>.
// */
//
// package fr.gaellalire.vestige.application.manager.secure;
//
// import java.security.PrivilegedAction;
//
// import fr.gaellalire.vestige.spi.job.JobHelper;
// import fr.gaellalire.vestige.spi.job.TaskHelper;
// import fr.gaellalire.vestige.spi.system.VestigeSystem;
//
/// **
// * @author Gael Lalire
// */
// public class SecureJobHelper implements JobHelper {
//
// private VestigeSystem vestigeSystem;
//
// private JobHelper delegate;
//
// public SecureJobHelper(final VestigeSystem vestigeSystem, final JobHelper delegate) {
// this.vestigeSystem = vestigeSystem;
// this.delegate = delegate;
// }
//
// @Override
// public TaskHelper addTask(final String taskDescription) {
// return new SecureTaskHelper(vestigeSystem, vestigeSystem.doPrivileged(new PrivilegedAction<TaskHelper>() {
//
// @Override
// public TaskHelper run() {
// return delegate.addTask(taskDescription);
// }
// }));
// }
//
// }