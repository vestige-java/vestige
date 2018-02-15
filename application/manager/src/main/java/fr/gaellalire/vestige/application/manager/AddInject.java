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

/**
 * @author Gael Lalire
 */
public class AddInject implements Serializable {

    private static final long serialVersionUID = 4240979926118037808L;

    private String serviceClassName;

    private String setterName;

    private String targetServiceClassName;

    public AddInject(final String serviceClassName, final String targetServiceClassName, final String setterName) {
        this.serviceClassName = serviceClassName;
        this.targetServiceClassName = targetServiceClassName;
        this.setterName = setterName;
    }

    public String getServiceClassName() {
        return serviceClassName;
    }

    public String getTargetServiceClassName() {
        if (targetServiceClassName == null) {
            return serviceClassName;
        }
        return targetServiceClassName;
    }

    public String getSetterName() {
        return setterName;
    }

}
