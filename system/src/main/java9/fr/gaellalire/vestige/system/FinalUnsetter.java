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

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.concurrent.Callable;

/**
 * @author Gael Lalire
 */
public final class FinalUnsetter {

    private FinalUnsetter() {
    }

    public static <E> E unsetFinalField(final Field field, final Callable<E> callable) throws Exception {
        Lookup privateLookupIn = MethodHandles.privateLookupIn(Field.class, MethodHandles.lookup());
        VarHandle findVarHandle = privateLookupIn.findVarHandle(Field.class, "modifiers", int.class);
        int modifierValue = ((Integer) findVarHandle.get(field)).intValue();
        findVarHandle.set(field, modifierValue & ~Modifier.FINAL);
        try {
            return callable.call();
        } finally {
            findVarHandle.set(field, modifierValue);
        }
    }

}
