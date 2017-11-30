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
// import fr.gaellalire.vestige.spi.job.TaskHelper;
// import fr.gaellalire.vestige.spi.system.VestigeSystem;
//
/// **
// * @author Gael Lalire
// */
// public class SecureTaskHelper implements TaskHelper {
//
// private VestigeSystem vestigeSystem;
//
// private TaskHelper delegate;
//
// public SecureTaskHelper(final VestigeSystem vestigeSystem, final TaskHelper delegate) {
// this.vestigeSystem = vestigeSystem;
// this.delegate = delegate;
// }
//
// @Override
// public void setProgress(final float progress) {
// vestigeSystem.doPrivileged(new PrivilegedAction<Void>() {
//
// @Override
// public Void run() {
// delegate.setProgress(progress);
// return null;
// }
// });
// }
//
// @Override
// public void setDone() {
// vestigeSystem.doPrivileged(new PrivilegedAction<Void>() {
//
// @Override
// public Void run() {
// delegate.setDone();
// return null;
// }
// });
// }
//
// }
