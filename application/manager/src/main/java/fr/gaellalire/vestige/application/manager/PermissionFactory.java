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

package fr.gaellalire.vestige.application.manager;

import java.io.Serializable;
import java.security.Permission;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.gaellalire.vestige.utils.KeepExpandMapValueGetter;
import fr.gaellalire.vestige.utils.Property;
import fr.gaellalire.vestige.utils.SimpleValueGetter;

/**
 * @author Gael Lalire
 */
public class PermissionFactory implements Serializable {

    private static final long serialVersionUID = -9218588432238991954L;

    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionFactory.class);

    private Property<String> typeProperty;

    private Property<String> nameProperty;

    private Property<String> actionsProperty;

    private static final ClassLoader SYSTEM_CLASS_LOADER = ClassLoader.getSystemClassLoader();

    public PermissionFactory(final Property<String> typeProperty, final Property<String> nameProperty, final Property<String> actionsProperty) {
        this.typeProperty = typeProperty;
        this.nameProperty = nameProperty;
        this.actionsProperty = actionsProperty;
    }

    public boolean isDynamic() {
        KeepExpandMapValueGetter keepExpandMapValueGetter = new KeepExpandMapValueGetter();
        keepExpandMapValueGetter.getValue(typeProperty);
        keepExpandMapValueGetter.getValue(nameProperty);
        keepExpandMapValueGetter.getValue(actionsProperty);
        return !keepExpandMapValueGetter.getExpandMap().isEmpty();
    }

    public Permission createPermission() {
        try {
            String type = SimpleValueGetter.INSTANCE.getValue(typeProperty);
            String name = SimpleValueGetter.INSTANCE.getValue(nameProperty);
            String actions = SimpleValueGetter.INSTANCE.getValue(actionsProperty);
            Class<?> loadClass = SYSTEM_CLASS_LOADER.loadClass(type);
            if (name == null) {
                try {
                    return ((Permission) loadClass.newInstance());
                } catch (Exception e) {
                    LOGGER.trace("Exception", e);
                }
            }
            if (actions == null) {
                try {
                    return ((Permission) loadClass.getConstructor(String.class).newInstance(name));
                } catch (Exception e) {
                    LOGGER.trace("Exception", e);
                }
            }
            try {
                return ((Permission) loadClass.getConstructor(String.class, String.class).newInstance(name, actions));
            } catch (Exception e) {
                LOGGER.trace("Exception", e);
            }
            LOGGER.error("Permission issue");
        } catch (Exception e) {
            LOGGER.error("Permission issue", e);
        }
        return null;
    }
}
