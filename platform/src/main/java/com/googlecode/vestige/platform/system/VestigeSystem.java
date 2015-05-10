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

package com.googlecode.vestige.platform.system;

import java.io.InputStream;
import java.io.PrintStream;
import java.net.ContentHandler;
import java.net.ContentHandlerFactory;
import java.net.ProxySelector;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.security.Permission;
import java.security.Policy;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;
import java.util.WeakHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;

import com.googlecode.vestige.platform.system.interceptor.VestigePolicy;

/**
 * @author Gael Lalire
 */
public final class VestigeSystem implements PublicVestigeSystem {

    private VestigeSystemHolder vestigeSystemHolder;

    private Hashtable<String, URLStreamHandler> urlStreamHandlerByProtocol;

    private Hashtable<String, ContentHandler> urlConnectionContentHandlerByMime;

    private URLStreamHandlerFactory urlStreamHandlerFactory;

    private ContentHandlerFactory urlConnectionContentHandlerFactory;

    private PrintStream out;

    private PrintStream err;

    private InputStream in;

    private Properties properties;

    private Properties securityProperties;

    private CopyOnWriteArrayList<Object> registeredDrivers;

    private Vector<Object> writeDrivers;

    private Map<Object, Vector<Object>> readDrivers;

    private ProxySelector defaultProxySelector;

    private VestigeSystemSecurityManager securityManager;

    private VestigePolicy previousWhiteListPolicy;

    private VestigePolicy previousBlackListPolicy;

    private VestigePolicy whiteListPolicy;

    private VestigePolicy blackListPolicy;

    private Object securityProviderList;

    private ArrayList<Level> knownLevels;

    private HashMap<String, List<Object>> nameToLevels;

    private HashMap<Integer, List<Object>> intToLevels;

    private void init(final VestigeSystemSecurityManager previousVestigeSecurityManager) {
        this.securityManager = new VestigeSystemSecurityManager(vestigeSystemHolder, this, previousVestigeSecurityManager);
        this.whiteListPolicy = new VestigePolicy(null) {
            @Override
            public Policy getCurrentPolicy() {
                // not used
                return null;
            }

            @Override
            public boolean implies(final ProtectionDomain domain, final Permission permission) {
                VestigeSystem currentVestigeSystem = vestigeSystemHolder.getVestigeSystem();
                vestigeSystemHolder.setVestigeSystem(VestigeSystem.this);
                try {
                    if (previousBlackListPolicy != null && !previousBlackListPolicy.implies(domain, permission)) {
                        return false;
                    }
                    Policy nextHandler = getNextHandler();
                    if (nextHandler != null && nextHandler.implies(domain, permission)) {
                        return true;
                    }
                    return false;
                } finally {
                    vestigeSystemHolder.setVestigeSystem(currentVestigeSystem);
                }
            }
        };
        this.blackListPolicy = new VestigePolicy(null) {
            @Override
            public Policy getCurrentPolicy() {
                return getNextHandler();
            }

            @Override
            public boolean implies(final ProtectionDomain domain, final Permission permission) {
                VestigeSystem currentVestigeSystem = vestigeSystemHolder.getVestigeSystem();
                vestigeSystemHolder.setVestigeSystem(VestigeSystem.this);
                try {
                    if (previousWhiteListPolicy != null && previousWhiteListPolicy.implies(domain, permission)) {
                        return true;
                    }
                    Policy nextHandler;
                    if (previousBlackListPolicy != null) {
                        nextHandler = previousBlackListPolicy.getNextHandler();
                        if (nextHandler != null && !nextHandler.implies(domain, permission)) {
                            return false;
                        }
                    }
                    nextHandler = whiteListPolicy.getNextHandler();
                    if (nextHandler != null && nextHandler.implies(domain, permission)) {
                        return true;
                    }
                    nextHandler = getNextHandler();
                    if (nextHandler != null && !nextHandler.implies(domain, permission)) {
                        return false;
                    }
                    return true;
                } finally {
                    vestigeSystemHolder.setVestigeSystem(currentVestigeSystem);
                }
            }
        };
    }

    public VestigeSystem(final VestigeSystemHolder vestigeSystemHolder) {
        this.vestigeSystemHolder = vestigeSystemHolder;
        init(null);
    }

    private static Properties copyProperties(final Properties previous) {
        if (previous == null) {
            return null;
        }
        Properties defaults = null;
        for (Object opropertyName : Collections.list(previous.propertyNames())) {
            if (!previous.containsKey(opropertyName)) {
                if (defaults == null) {
                    defaults = new Properties();
                }
                if (opropertyName instanceof String) {
                    String propertyName = (String) opropertyName;
                    defaults.put(propertyName, previous.getProperty(propertyName));
                }
            }
        }
        Properties properties = new Properties(defaults);
        properties.putAll(previous);
        return properties;
    }

    @SuppressWarnings("unchecked")
    public VestigeSystem(final VestigeSystem previousSystem) {
        vestigeSystemHolder = previousSystem.vestigeSystemHolder;
        init(previousSystem.securityManager);
        previousWhiteListPolicy = previousSystem.whiteListPolicy;
        previousBlackListPolicy = previousSystem.blackListPolicy;
        // URL
        if (previousSystem.urlStreamHandlerByProtocol != null) {
            urlStreamHandlerByProtocol = new Hashtable<String, URLStreamHandler>();
            urlStreamHandlerByProtocol.putAll(previousSystem.urlStreamHandlerByProtocol);
        }
        urlStreamHandlerFactory = previousSystem.urlStreamHandlerFactory;
        // URLConnection
        if (previousSystem.urlConnectionContentHandlerByMime != null) {
            urlConnectionContentHandlerByMime = new Hashtable<String, ContentHandler>();
            urlConnectionContentHandlerByMime.putAll(previousSystem.urlConnectionContentHandlerByMime);
        }
        urlConnectionContentHandlerFactory = previousSystem.urlConnectionContentHandlerFactory;
        // System
        out = previousSystem.out;
        err = previousSystem.err;
        in = previousSystem.in;
        properties = copyProperties(previousSystem.properties);
        securityProperties = copyProperties(previousSystem.securityProperties);
        // no need to clone, ProviderList instance are immutable
        securityProviderList = previousSystem.securityProviderList;
        // DriverManager
        if (previousSystem.writeDrivers != null) {
            writeDrivers = (Vector<Object>) previousSystem.writeDrivers.clone();
            readDrivers = new WeakHashMap<Object, Vector<Object>>();
            readDrivers.put(this, (Vector<Object>) writeDrivers.clone());
        }
        if (previousSystem.registeredDrivers != null) {
            registeredDrivers = new CopyOnWriteArrayList<Object>(previousSystem.registeredDrivers);
        }
        // ProxySelector
        defaultProxySelector = previousSystem.defaultProxySelector;
        // known level
        if (previousSystem.knownLevels != null) {
            knownLevels = (ArrayList<Level>) previousSystem.knownLevels.clone();
        }
        if (previousSystem.nameToLevels != null) {
            nameToLevels = (HashMap<String, List<Object>>) previousSystem.nameToLevels.clone();
        }
        if (previousSystem.intToLevels != null) {
            intToLevels = (HashMap<Integer, List<Object>>) previousSystem.intToLevels.clone();
        }
    }

    public void setWriteDrivers(final Vector<Object> writeDrivers) {
        this.writeDrivers = writeDrivers;
    }

    public Vector<Object> getWriteDrivers() {
        return writeDrivers;
    }

    public void setReadDrivers(final Map<Object, Vector<Object>> readDrivers) {
        this.readDrivers = readDrivers;
    }

    public Map<Object, Vector<Object>> getReadDrivers() {
        return readDrivers;
    }

    public CopyOnWriteArrayList<Object> getRegisteredDrivers() {
        return registeredDrivers;
    }

    public URLStreamHandlerFactory getURLStreamHandlerFactory() {
        return urlStreamHandlerFactory;
    }

    public Properties getSecurityProperties() {
        return securityProperties;
    }

    public Properties getProperties() {
        return properties;
    }

    public PrintStream getOut() {
        return out;
    }

    public PrintStream getErr() {
        return err;
    }

    public InputStream getIn() {
        return in;
    }

    public ContentHandlerFactory getURLConnectionContentHandlerFactory() {
        return urlConnectionContentHandlerFactory;
    }

    public void setURLConnectionContentHandlerFactory(final ContentHandlerFactory contentHandlerFactory) {
        this.urlConnectionContentHandlerFactory = contentHandlerFactory;
    }

    public void setURLStreamHandlerFactory(final URLStreamHandlerFactory urlStreamHandlerFactory) {
        if (this.urlStreamHandlerFactory != null) {
            throw new Error("factory already defined");
        }
        this.urlStreamHandlerFactory = urlStreamHandlerFactory;
    }

    public void setOut(final PrintStream out) {
        this.out = out;
    }

    public void setErr(final PrintStream err) {
        this.err = err;
    }

    public void setIn(final InputStream in) {
        this.in = in;
    }

    public void setSecurityProperties(final Properties securityProperties) {
        this.securityProperties = securityProperties;
    }

    public void setProperties(final Properties properties) {
        this.properties = properties;
    }

    public ProxySelector getDefaultProxySelector() {
        return defaultProxySelector;
    }

    public void setDefaultProxySelector(final ProxySelector defaultProxySelector) {
        this.defaultProxySelector = defaultProxySelector;
    }

    public void setRegisteredDrivers(final CopyOnWriteArrayList<Object> registeredDrivers) {
        this.registeredDrivers = registeredDrivers;
    }

    public Hashtable<String, URLStreamHandler> getURLStreamHandlerByProtocol() {
        return urlStreamHandlerByProtocol;
    }

    public void setURLStreamHandlerByProtocol(final Hashtable<String, URLStreamHandler> urlStreamHandlerByProtocol) {
        this.urlStreamHandlerByProtocol = urlStreamHandlerByProtocol;
    }

    public Hashtable<String, ContentHandler> getURLConnectionContentHandlerByMime() {
        return urlConnectionContentHandlerByMime;
    }

    public void setURLConnectionContentHandlerByMime(final Hashtable<String, ContentHandler> urlConnectionContentHandlerByMime) {
        this.urlConnectionContentHandlerByMime = urlConnectionContentHandlerByMime;
    }

    public VestigeSystemSecurityManager getCurrentSecurityManager() {
        return securityManager;
    }

    public SecurityManager getSecurityManager() {
        return securityManager.getSecurityManager();
    }

    public void setSecurityManager(final SecurityManager securityManager) {
        this.securityManager.setSecurityManager(securityManager);
    }

    public Policy getWhiteListPolicy() {
        return whiteListPolicy.getNextHandler();
    }

    public Policy getPolicy() {
        return blackListPolicy.getNextHandler();
    }

    public VestigePolicy getCurrentPolicy() {
        return blackListPolicy;
    }

    public void setWhiteListPolicy(final Policy whiteListPolicy) {
        this.whiteListPolicy.setNextHandler(whiteListPolicy);
    }

    public void setPolicy(final Policy blackListPolicy) {
        this.blackListPolicy.setNextHandler(blackListPolicy);
    }

    @Override
    public PublicVestigeSystem createSubSystem() {
        return new VestigeSystem(this);
    }

    @Override
    public void setCurrentSystem() {
        vestigeSystemHolder.setVestigeSystem(this);
    }

    public Object getSecurityProviderList() {
        return securityProviderList;
    }

    public void setSecurityProviderList(final Object securityProviderList) {
        this.securityProviderList = securityProviderList;
    }

    public ArrayList<Level> getKnownLevels() {
        return knownLevels;
    }

    public void setKnownLevels(final ArrayList<Level> knownLevels) {
        this.knownLevels = knownLevels;
    }

    public void setNameToLevels(final HashMap<String, List<Object>> nameToLevels) {
        this.nameToLevels = nameToLevels;
    }

    public void setIntToLevels(final HashMap<Integer, List<Object>> intToLevels) {
        this.intToLevels = intToLevels;
    }

    public HashMap<String, List<Object>> getNameToLevels() {
        return nameToLevels;
    }

    public HashMap<Integer, List<Object>> getIntToLevels() {
        return intToLevels;
    }

}
