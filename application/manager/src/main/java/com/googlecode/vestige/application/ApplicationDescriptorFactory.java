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

package com.googlecode.vestige.application;

import java.net.URL;
import java.util.List;

/**
 * @author Gael Lalire
 */
public interface ApplicationDescriptorFactory {

    boolean hasApplicationDescriptor(final URL context, final String repoName, final String appName, final List<Integer> version) throws ApplicationException;

    ApplicationDescriptor createApplicationDescriptor(final URL context, String repoName, final String appName, final List<Integer> version) throws ApplicationException;

}
