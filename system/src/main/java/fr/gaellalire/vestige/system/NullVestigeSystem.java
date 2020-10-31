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

import java.io.InputStream;
import java.io.PrintStream;
import java.net.ContentHandlerFactory;
import java.net.ProxySelector;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.security.Policy;
import java.util.Properties;

import fr.gaellalire.vestige.spi.system.VestigeSystem;
import fr.gaellalire.vestige.spi.system.VestigeSystemCache;

/**
 * @author Gael Lalire
 */
public class NullVestigeSystem implements VestigeSystem {

    private VestigeSystemHolder vestigeSystemHolder;

    public NullVestigeSystem(final VestigeSystemHolder vestigeSystemHolder) {
        this.vestigeSystemHolder = vestigeSystemHolder;
    }

    @Override
    public Properties getProperties() {
        return null;
    }

    @Override
    public void setProperties(final Properties properties) {
    }

    @Override
    public PrintStream getOut() {
        return null;
    }

    @Override
    public void setOut(final PrintStream out) {
    }

    @Override
    public PrintStream getErr() {
        return null;
    }

    @Override
    public void setErr(final PrintStream err) {
    }

    @Override
    public InputStream getIn() {
        return null;
    }

    @Override
    public void setIn(final InputStream in) {
    }

    @Override
    public void setURLStreamHandlerFactory(final URLStreamHandlerFactory urlStreamHandlerFactory) {
    }

    @Override
    public void setURLStreamHandlerForProtocol(final String protocol, final URLStreamHandler urlStreamHandler) {
    }

    @Override
    public ContentHandlerFactory getURLConnectionContentHandlerFactory() {
        return null;
    }

    @Override
    public void setURLConnectionContentHandlerFactory(final ContentHandlerFactory contentHandlerFactory) {
    }

    @Override
    public ProxySelector getDefaultProxySelector() {
        return null;
    }

    @Override
    public void setDefaultProxySelector(final ProxySelector defaultProxySelector) {
    }

    @Override
    public void setWhiteListPolicy(final Policy policy) {
    }

    @Override
    public Policy getWhiteListPolicy() {
        return null;
    }

    @Override
    public void setPolicy(final Policy policy) {
    }

    @Override
    public Policy getPolicy() {
        return null;
    }

    @Override
    public void setSecurityManager(final SecurityManager policy) {
    }

    @Override
    public SecurityManager getSecurityManager() {
        return null;
    }

    @Override
    public VestigeSystem createSubSystem(final String name) {
        return null;
    }

    @Override
    public VestigeSystem setCurrentSystem() {
        return vestigeSystemHolder.setVestigeSystem(null);
    }

    @Override
    public VestigeSystemCache pushVestigeSystemCache() {
        return null;
    }

    @Override
    public URLStreamHandlerFactory getURLStreamHandlerFactory() {
        return null;
    }

}
