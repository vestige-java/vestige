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

package fr.gaellalire.vestige.resolver.maven;

import fr.gaellalire.vestige.platform.JPMSNamedModulesConfiguration;
import fr.gaellalire.vestige.spi.resolver.Scope;

/**
 * @author Gael Lalire
 */
public class CreateClassLoaderConfigurationParameters {

    private String appName;

    private DefaultJPMSConfiguration jpmsConfiguration;

    private JPMSNamedModulesConfiguration jpmsNamedModulesConfiguration;

    private boolean manyLoaders;

    private Scope scope;

    private ScopeModifier scopeModifier;

    private BeforeParentController beforeParentController;

    private boolean selfExcluded;

    public String getAppName() {
        return appName;
    }

    public void setAppName(final String appName) {
        this.appName = appName;
    }

    public DefaultJPMSConfiguration getJpmsConfiguration() {
        return jpmsConfiguration;
    }

    public void setJpmsConfiguration(final DefaultJPMSConfiguration jpmsConfiguration) {
        this.jpmsConfiguration = jpmsConfiguration;
    }

    public JPMSNamedModulesConfiguration getJpmsNamedModulesConfiguration() {
        return jpmsNamedModulesConfiguration;
    }

    public void setJpmsNamedModulesConfiguration(final JPMSNamedModulesConfiguration jpmsNamedModulesConfiguration) {
        this.jpmsNamedModulesConfiguration = jpmsNamedModulesConfiguration;
    }

    public boolean isManyLoaders() {
        return manyLoaders;
    }

    public void setManyLoaders(final boolean manyLoaders) {
        this.manyLoaders = manyLoaders;
    }

    public Scope getScope() {
        return scope;
    }

    public void setScope(final Scope scope) {
        this.scope = scope;
    }

    public ScopeModifier getScopeModifier() {
        return scopeModifier;
    }

    public void setScopeModifier(final ScopeModifier scopeModifier) {
        this.scopeModifier = scopeModifier;
    }

    public BeforeParentController getBeforeParentController() {
        return beforeParentController;
    }

    public void setBeforeParentController(final BeforeParentController beforeParentController) {
        this.beforeParentController = beforeParentController;
    }

    public boolean isSelfExcluded() {
        return selfExcluded;
    }

    public void setSelfExcluded(final boolean selfExcluded) {
        this.selfExcluded = selfExcluded;
    }

}
