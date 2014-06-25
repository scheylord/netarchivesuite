/*
 * #%L
 * Netarchivesuite - monitor - test
 * %%
 * Copyright (C) 2005 - 2014 The Royal Danish Library, the Danish State and University Library,
 *             the National Library of France and the Austrian National Library.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 2.1 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */
package dk.netarkivet.monitor;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import junit.framework.TestCase;

/** Unittestersuite for the MonitorSettings class. */
public class MonitorSettingsTester extends TestCase {

    public void testNoFinalSettingsConstants() {
        Class c = MonitorSettings.class;
        Field[] fields = c.getDeclaredFields();
        for (Field f: fields) {
            // Check that all static public fields are not final
            int modifiers = f.getModifiers();
            if (Modifier.isPublic(modifiers) && Modifier.isStatic(modifiers)) {
                assertFalse("public static fields must not be final, but this was violated by field " + f.getName(),            
                         Modifier.isFinal(modifiers));
            }
        }
    }
}
