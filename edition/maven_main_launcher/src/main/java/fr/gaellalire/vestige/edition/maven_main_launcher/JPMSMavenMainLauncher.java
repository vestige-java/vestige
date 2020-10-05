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

package fr.gaellalire.vestige.edition.maven_main_launcher;

import java.lang.ModuleLayer.Controller;
import java.net.URL;
import java.util.List;

import fr.gaellalire.vestige.core.VestigeCoreContext;
import fr.gaellalire.vestige.core.function.Function;

/**
 * @author Gael Lalire
 */
public final class JPMSMavenMainLauncher {

    private JPMSMavenMainLauncher() {
    }

    public static void vestigeEnhancedCoreMain(final VestigeCoreContext vestigeCoreContext, final Function<Thread, Void, RuntimeException> addShutdownHook,
            final Function<Thread, Void, RuntimeException> removeShutdownHook, final List<? extends ClassLoader> privilegedClassloaders, final Controller controller,
            final String[] args) {
        // controller is just useless because this classloader is wanted to be GC
        MavenMainLauncher.vestigeEnhancedCoreMain(vestigeCoreContext, addShutdownHook, removeShutdownHook, privilegedClassloaders, args);
    }

    public static void vestigeCoreMain(final VestigeCoreContext vestigeCoreContext, final Controller controller, final String[] args) throws Exception {
        vestigeEnhancedCoreMain(vestigeCoreContext, null, null, null, controller, args);
    }

    public static void vestigeCoreMain(final VestigeCoreContext vestigeCoreContext, final String[] args) throws Exception {
        vestigeCoreMain(vestigeCoreContext, null, args);
    }

    public static void main(final String[] args) throws Exception {
        VestigeCoreContext vestigeCoreContext = VestigeCoreContext.buildDefaultInstance();
        URL.setURLStreamHandlerFactory(vestigeCoreContext.getStreamHandlerFactory());
        vestigeCoreMain(vestigeCoreContext, args);
    }

}
