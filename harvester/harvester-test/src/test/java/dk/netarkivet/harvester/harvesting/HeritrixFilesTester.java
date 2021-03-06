/*
 * #%L
 * Netarchivesuite - harvester - test
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
package dk.netarkivet.harvester.harvesting;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.harvester.datamodel.HeritrixTemplate;
import dk.netarkivet.testutils.preconfigured.MockupJMS;

/**
 * Unittests for the class HeritrixFiles.
 */
public class HeritrixFilesTester {

    private MockupJMS mjms = new MockupJMS();

    private File defaultJmxPasswordFile = new File("/path/to/jmxpasswordfile");
    private File defaultJmxAccessFile = new File("/path/to/jmxaccessfile");

    @Before
    public void setUp() {
        TestInfo.WORKING_DIR.mkdirs();
        mjms.setUp();
    }

    @After
    public void tearDown() {
        mjms.tearDown();
        FileUtils.removeRecursively(TestInfo.WORKING_DIR);
    }

    @Test(expected = ArgumentNotValid.class)
    public void testConstructorWithNull() {
        new HeritrixFiles(null, null, null, null);
    }

    /**
     * Test correct behaviour of the HeritrixFiles constructor.
     */
    @Test
    public void testConstructor() {
        TestInfo.HERITRIX_TEMP_DIR.mkdir();
        HeritrixFiles hf = getStandardHeritrixFiles();

        // check, that crawlDir is correctly set
        assertEquals("crawlDir should be set up correctly.", TestInfo.HERITRIX_TEMP_DIR.getAbsolutePath(), hf
                .getCrawlDir().getAbsolutePath());

        // check, that arcFilePrefix is correctly set
        assertEquals("arcFilePrefix should contain job id and harvest id", "42-42", hf.getArchiveFilePrefix());
        assertEquals("jmxPasswordFile should be" + defaultJmxPasswordFile.getAbsolutePath(), defaultJmxPasswordFile,
                hf.getJmxPasswordFile());
        assertEquals("jmxAccessfile should be" + defaultJmxAccessFile.getAbsolutePath(), defaultJmxAccessFile,
                hf.getJmxAccessFile());

    }

    /**
     * Test alternate constructor.
     */
    @Test
    public void testAlternateConstructor() {
        HeritrixFiles hf = HeritrixFiles.getH1HeritrixFilesWithDefaultJmxFiles(TestInfo.HERITRIX_TEMP_DIR, new JobInfoTestImpl(42L, 42L));
        // check, that crawlDir is correctly set
        assertEquals("crawlDir should be set up correctly.", TestInfo.HERITRIX_TEMP_DIR.getAbsolutePath(), hf
                .getCrawlDir().getAbsolutePath());

        // check, that arcFilePrefix is correctly set
        assertEquals("arcFilePrefix should contain job id and harvest id", "42-42", hf.getArchiveFilePrefix());

        // check, that in the alternate constructor the JMX files to
        // be used by Heritrix is read from settings.
        File jmxPasswordFileFromSettings = new File(Settings.get(CommonSettings.JMX_PASSWORD_FILE));
        File jmxAccessFileFromSettings = new File(Settings.get(CommonSettings.JMX_ACCESS_FILE));

        assertEquals("jmxPasswordFile should be" + jmxPasswordFileFromSettings.getAbsolutePath(),
                jmxPasswordFileFromSettings, hf.getJmxPasswordFile());
        assertEquals("jmxAccessfile should be" + jmxAccessFileFromSettings.getAbsolutePath(),
                jmxAccessFileFromSettings, hf.getJmxAccessFile());
    }

    /**
     * Test, that writeOrderXml fails correctly with bad arguments: - null argument - Document object with no contents.
     * <p>
     * Bug 871 caused this test to be written.
     */
    @Test
    public void testWriteOrderXml() {
        TestInfo.HERITRIX_TEMP_DIR.mkdir();
        HeritrixFiles hf = getStandardHeritrixFiles();
        try {
            hf.writeOrderXml(null);
            fail("ArgumentNotValid exception with null Document");
        } catch (ArgumentNotValid e) {
            // Expected
        }
        
   
        // test, that order xml is written, if argument is valid

        //Document doc = XmlUtils.getXmlDoc(TestInfo.ORDER_FILE);
        File orderFile = new File("tests/dk/netarkivet/harvester/scheduler/data/originals/order.xml");

        //HeritrixTemplate doc = HeritrixTemplate.read(TestInfo.ORDER_FILE);
        HeritrixTemplate doc = HeritrixTemplate.read(orderFile);
        try {
            hf.writeOrderXml(doc);
        } catch (Exception e) {
            fail("Exception not expected: " + e);
        }
    }

    /**
     * Test, that writeSeedsTxt fails correctly with bad arguments: - null argument - empty String
     * <p>
     * Bug 871 caused this test to be written.
     */
    @Test
    public void testWriteSeedsTxt() {
        TestInfo.HERITRIX_TEMP_DIR.mkdir();
        HeritrixFiles hf = getStandardHeritrixFiles();
        try {
            hf.writeSeedsTxt(null);
            fail("ArgumentNotValid exception with null seeds");
        } catch (ArgumentNotValid e) {
            // Expected
        }

        try {
            hf.writeSeedsTxt("");
            fail("ArgumentNotValid exception with seeds equal to empty string");
        } catch (ArgumentNotValid e) {
            // Expected
        }

        try {
            hf.writeSeedsTxt("www.netarkivet.dk\nwww.sulnudu.dk");
        } catch (Exception e) {
            fail("Exception not expected with seeds equal to valid non-empty String object" + e);
        }
    }

    /** Check, that the getArcsDir method works. */
    @Test
    public void testGetArcsDir() {
        TestInfo.HERITRIX_TEMP_DIR.mkdir();
        HeritrixFiles hf = getStandardHeritrixFiles();
        File arcsdir = hf.getArcsDir();
        assertEquals("Wrong arcsdir", new File(TestInfo.HERITRIX_TEMP_DIR,
                dk.netarkivet.common.Constants.ARCDIRECTORY_NAME), arcsdir);
    }

    /** Check the getHeritrixOutput method */
    @Test
    public void testGetHeritrixOutput() {
        TestInfo.HERITRIX_TEMP_DIR.mkdir();
        HeritrixFiles hf = getStandardHeritrixFiles();
        File output = hf.getHeritrixOutput();
        assertEquals("Wrong heritrixOutputDir", new File(hf.getCrawlDir(), "heritrix.out"), output);
    }

    /**
     * Standard HeritrixFiles setup with crawldir = TestInfo.HERITRIX_TEMP_DIR,
     * jobID=42,harvestID=42,jmxPasswordFile/jmxAccessFile defined as /path/to/jmxpasswordfile and
     * /path/to/jmxaccessfile respectively
     *
     * @return
     */
    public HeritrixFiles getStandardHeritrixFiles() {
        return new HeritrixFiles(TestInfo.HERITRIX_TEMP_DIR, new JobInfoTestImpl(42L, 42L), new File(
                "/path/to/jmxpasswordfile"), new File("/path/to/jmxaccessfile"));
    }
}
