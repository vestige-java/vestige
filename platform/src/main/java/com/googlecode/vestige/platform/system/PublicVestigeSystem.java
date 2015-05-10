/*
 * This file is part of Vestige.
 *
 * Vestige is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General  License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Vestige is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General  License for more details.
 *
 * You should have received a copy of the GNU General  License
 * along with Vestige.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.googlecode.vestige.platform.system;

import java.io.InputStream;
import java.io.PrintStream;
import java.net.ContentHandlerFactory;
import java.net.ProxySelector;
import java.net.URLStreamHandlerFactory;
import java.security.Policy;
import java.util.Properties;

/**
 * @author Gael Lalire
 */
public interface PublicVestigeSystem {

    Properties getProperties();

    void setProperties(Properties properties);

    PrintStream getOut();

    void setOut(PrintStream out);

    PrintStream getErr();

    void setErr(PrintStream err);

    InputStream getIn();

    void setIn(InputStream in);

    void setURLStreamHandlerFactory(URLStreamHandlerFactory urlStreamHandlerFactory);

    ContentHandlerFactory getURLConnectionContentHandlerFactory();

    void setURLConnectionContentHandlerFactory(ContentHandlerFactory contentHandlerFactory);

    ProxySelector getDefaultProxySelector();

    void setDefaultProxySelector(ProxySelector defaultProxySelector);

    void setWhiteListPolicy(Policy policy);

    Policy getWhiteListPolicy();

    void setPolicy(Policy policy);

    Policy getPolicy();

    void setSecurityManager(SecurityManager policy);

    SecurityManager getSecurityManager();

    PublicVestigeSystem createSubSystem();

    void setCurrentSystem();

}
