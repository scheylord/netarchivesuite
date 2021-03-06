/*
 * #%L
 * Netarchivesuite - common - test
 * %%
 * Copyright (C) 2005 - 2017 The Royal Danish Library, 
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
package dk.netarkivet.common.utils;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

/**
 * Unit tests for the {@link ReadOnlyByteArray} class.
 */
public class ReadOnlyByteArrayTester {

    @Test
    public void testClassFunctionality() {

        new ReadOnlyByteArray(null);

        byte[] emptyArray = new byte[] {};
        ReadOnlyByteArray roba = new ReadOnlyByteArray(emptyArray);
        assertTrue(roba.length() == 0);
        try {
            roba.get(0);
            fail("roba.get(0) should not be accepted");
        } catch (Exception e) {
            // Expected
        }

        byte[] notEmptyArray = new byte[] {22, 42};
        roba = new ReadOnlyByteArray(notEmptyArray);
        assertTrue(roba.length() == 2);
        assertTrue(22 == roba.get(0));
        assertTrue(42 == roba.get(1));
    }

}
