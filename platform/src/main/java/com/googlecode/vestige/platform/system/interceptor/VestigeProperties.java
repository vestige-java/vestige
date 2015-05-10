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

package com.googlecode.vestige.platform.system.interceptor;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Collection;
import java.util.Enumeration;
import java.util.InvalidPropertiesFormatException;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import com.googlecode.vestige.core.StackedHandler;

/**
 * @author Gael Lalire
 */
public abstract class VestigeProperties extends Properties implements StackedHandler<Properties> {

    private static final long serialVersionUID = 361157322432941610L;

    private Properties nextHandler;

    public VestigeProperties(final Properties properties) {
        this.nextHandler = properties;
    }

    public abstract Properties getProperties();

    @Override
    public Object put(final Object key, final Object value) {
        return getProperties().put(key, value);
    }

    @Override
    public Object get(final Object key) {
        return getProperties().get(key);
    }

    @Override
    public String getProperty(final String key) {
        return getProperties().getProperty(key);
    }

    @Override
    public Object setProperty(final String key, final String value) {
        return getProperties().setProperty(key, value);
    }

    @Override
    public boolean contains(final Object value) {
        return getProperties().contains(value);
    }

    @Override
    public boolean containsKey(final Object key) {
        return getProperties().containsKey(key);
    }

    @Override
    public Enumeration<Object> elements() {
        return getProperties().elements();
    }

    @Override
    public Set<java.util.Map.Entry<Object, Object>> entrySet() {
        return getProperties().entrySet();
    }

    @Override
    public Collection<Object> values() {
        return getProperties().values();
    }

    @Override
    public boolean isEmpty() {
        return getProperties().isEmpty();
    }

    @Override
    public Set<Object> keySet() {
        return getProperties().keySet();
    }

    @Override
    public String toString() {
        return getProperties().toString();
    }

    @Override
    public int size() {
        return getProperties().size();
    }

    @Override
    public void clear() {
        getProperties().clear();
    }

    @Override
    public Object remove(final Object key) {
        return getProperties().remove(key);
    }

    @Override
    public Enumeration<Object> keys() {
        return getProperties().keys();
    }

    @Override
    public Object clone() {
        return getProperties().clone();
    }

    @Override
    public boolean containsValue(final Object value) {
        return getProperties().containsValue(value);
    }

    @Override
    public boolean equals(final Object o) {
        return getProperties().equals(o);
    }

    @Override
    public String getProperty(final String key, final String defaultValue) {
        return getProperties().getProperty(key, defaultValue);
    }

    @Override
    public synchronized int hashCode() {
        return getProperties().hashCode();
    }

    @Override
    public void list(final PrintStream out) {
        getProperties().list(out);
    }

    @Override
    public void list(final PrintWriter out) {
        getProperties().list(out);
    }

    @Override
    public void load(final InputStream inStream) throws IOException {
        getProperties().load(inStream);
    }

    @Override
    public void load(final Reader reader) throws IOException {
        getProperties().load(reader);
    }

    @Override
    public void loadFromXML(final InputStream in) throws IOException, InvalidPropertiesFormatException {
        getProperties().loadFromXML(in);
    }

    @Override
    public Enumeration<?> propertyNames() {
        return getProperties().propertyNames();
    }

    @Override
    public void putAll(final Map<? extends Object, ? extends Object> t) {
        getProperties().putAll(t);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void save(final OutputStream out, final String comments) {
        super.save(out, comments);
    }

    @Override
    public void store(final OutputStream out, final String comments) throws IOException {
        getProperties().store(out, comments);
    }

    @Override
    public void store(final Writer writer, final String comments) throws IOException {
        getProperties().store(writer, comments);
    }

    @Override
    public void storeToXML(final OutputStream os, final String comment) throws IOException {
        getProperties().storeToXML(os, comment);
    }

    @Override
    public void storeToXML(final OutputStream os, final String comment, final String encoding) throws IOException {
        getProperties().storeToXML(os, comment, encoding);
    }

    @Override
    public Set<String> stringPropertyNames() {
        return getProperties().stringPropertyNames();
    }

    @Override
    public Properties getNextHandler() {
        return nextHandler;
    }

    @Override
    public void setNextHandler(final Properties nextHandler) {
        this.nextHandler = nextHandler;
    }

}
