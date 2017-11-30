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

package fr.gaellalire.vestige.system;

/**
 * @author Gael Lalire
 */
public class VestigeSystemHolder {

    private ThreadLocal<DefaultVestigeSystem> vestigeSystems = new InheritableThreadLocal<DefaultVestigeSystem>();

    private DefaultVestigeSystem fallbackVestigeSystem;

    private VestigeSystemJarURLStreamHandler vestigeApplicationJarURLStreamHandler;

    public DefaultVestigeSystem getVestigeSystem() {
        DefaultVestigeSystem vestigeSystem = vestigeSystems.get();
        if (vestigeSystem != null) {
            return vestigeSystem;
        }
        return fallbackVestigeSystem;
    }

    public DefaultVestigeSystem setVestigeSystem(final DefaultVestigeSystem vestigeSystem) {
        DefaultVestigeSystem pushedDefaultVestigeSystem = vestigeSystems.get();
        vestigeSystems.set(vestigeSystem);
        return pushedDefaultVestigeSystem;
    }

    public void setFallbackVestigeSystem(final DefaultVestigeSystem vestigeSystem) {
        fallbackVestigeSystem = vestigeSystem;
    }

    public VestigeSystemJarURLStreamHandler getVestigeApplicationJarURLStreamHandler() {
        return vestigeApplicationJarURLStreamHandler;
    }

    public void setVestigeApplicationJarURLStreamHandler(final VestigeSystemJarURLStreamHandler vestigeApplicationJarURLStreamHandler) {
        this.vestigeApplicationJarURLStreamHandler = vestigeApplicationJarURLStreamHandler;
    }

}
