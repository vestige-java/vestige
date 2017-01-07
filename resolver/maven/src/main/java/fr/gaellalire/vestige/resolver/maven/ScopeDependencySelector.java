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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.TreeSet;

import org.eclipse.aether.collection.DependencyCollectionContext;
import org.eclipse.aether.collection.DependencySelector;
import org.eclipse.aether.graph.Dependency;

/**
 * A dependency selector that filters transitive dependencies based on their
 * scope. Direct dependencies are always included regardless of their scope.
 * <em>Note:</em> This filter does not assume any relationships between the
 * scopes. In particular, the filter is not aware of scopes that logically
 * include other scopes.
 * @see Dependency#getScope()
 */
public final class ScopeDependencySelector implements DependencySelector {

    private final boolean transitive;

    private final Collection<String> included;

    private final Collection<String> excluded;

    /**
     * Creates a new selector using the specified includes and excludes.
     * @param included The set of scopes to include, may be {@code null} or empty
     *        to include any scope.
     * @param excluded The set of scopes to exclude, may be {@code null} or
     *        empty to exclude no scope.
     */
    public ScopeDependencySelector(final Collection<String> included, final Collection<String> excluded) {
        transitive = false;
        this.included = clone(included);
        this.excluded = clone(excluded);
    }

    private static Collection<String> clone(final Collection<String> scopes) {
        Collection<String> copy;
        if (scopes == null || scopes.isEmpty()) {
            // checking for null is faster than isEmpty()
            copy = null;
        } else {
            copy = new HashSet<String>(scopes);
            if (copy.size() <= 2) {
                // contains() is faster for smallish array (sorted for
                // equals()!)
                copy = new ArrayList<String>(new TreeSet<String>(copy));
            }
        }
        return copy;
    }

    /**
     * Creates a new selector using the specified excludes.
     * @param excluded The set of scopes to exclude, may be {@code null} or empty
     *        to exclude no scope.
     */
    public ScopeDependencySelector(final String... excluded) {
        this(null, toList(excluded));
    }

    private static <E> List<E> toList(final E[] array) {
        if (array == null) {
            return null;
        }
        return Arrays.asList(array);
    }

    private ScopeDependencySelector(final boolean transitive, final Collection<String> included, final Collection<String> excluded) {
        this.transitive = transitive;
        this.included = included;
        this.excluded = excluded;
    }

    public boolean selectDependency(final Dependency dependency) {
        if (!transitive) {
            return true;
        }

        String scope = dependency.getScope();
        return (included == null || included.contains(scope)) && (excluded == null || !excluded.contains(scope));
    }

    public DependencySelector deriveChildSelector(final DependencyCollectionContext context) {
        if (this.transitive || context.getDependency() == null) {
            return this;
        }

        return new ScopeDependencySelector(true, included, excluded);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        } else if (null == obj || !getClass().equals(obj.getClass())) {
            return false;
        }

        ScopeDependencySelector that = (ScopeDependencySelector) obj;
        return transitive == that.transitive && eq(included, that.included) && eq(excluded, that.excluded);
    }

    private static <T> boolean eq(final T o1, final T o2) {
        if (o1 != null) {
            return o1.equals(o2);
        }
        return o2 == null;
    }

    @Override
    public int hashCode() {
        int hash = 17;
        return hash;
    }

}
