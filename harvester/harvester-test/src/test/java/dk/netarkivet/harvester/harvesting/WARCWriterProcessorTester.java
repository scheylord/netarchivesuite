/*
 * #%L
 * Netarchivesuite - harvester - test
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
package dk.netarkivet.harvester.harvesting;

import java.io.File;

import org.dom4j.Document;

import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.XmlUtils;
import org.junit.Before;
import org.junit.After;
import org.junit.Test;

public class WARCWriterProcessorTester {

    private static final String DISK_PATH_XPATH = "//crawl-order/controller" + "/string[@name='disk-path']";

    String dirToTestWarcInfoWithScheduleName = "with-schedulename";
    String dirToTestWarcInfoWithoutScheduleName = "without-schedulename";
    File DirWith = new File(TestInfo.WORKING_DIR, dirToTestWarcInfoWithScheduleName);
    File DirWithout = new File(TestInfo.WORKING_DIR, dirToTestWarcInfoWithoutScheduleName);
    File order = new File(TestInfo.WARCPROCESSORFILES_DIR, "order_for_testing_warcinfo.xml");
    File harvestInfoWith = new File(TestInfo.WARCPROCESSORFILES_DIR, "harvestInfo.xml-with-scheduleName");
    File harvestInfoWithout = new File(TestInfo.WARCPROCESSORFILES_DIR, "harvestInfo.xml-without-scheduleName");

    File orderWithOut;
    File orderWith;

    @Before
    public void setUp() {
        TestInfo.WORKING_DIR.mkdirs();
        DirWith.mkdirs();
        DirWithout.mkdirs();
        FileUtils.copyFile(order, new File(DirWith, order.getName()));
        FileUtils.copyFile(order, new File(DirWithout, order.getName()));
        FileUtils.copyFile(harvestInfoWith, new File(DirWith, "harvestInfo.xml"));
        FileUtils.copyFile(harvestInfoWithout, new File(DirWithout, "harvestInfo.xml"));
        orderWith = new File(DirWith, "order_for_testing_warcinfo.xml");
        orderWithOut = new File(DirWithout, "order_for_testing_warcinfo.xml");
    }

    @After
    public void tearDown() {
        FileUtils.removeRecursively(TestInfo.WORKING_DIR);
    }

    @Test
    public void testWriteWarcInfoWithScheduleName() {
        // Change disk-path of order.xml to Dir With
        Document doc = XmlUtils.getXmlDoc(orderWith);

        XmlUtils.setNode(doc, DISK_PATH_XPATH, orderWith.getParentFile().getAbsolutePath());
        XmlUtils.writeXmlToFile(doc, orderWith);

        WARCWriterProcessor p = new WARCWriterProcessor("testing");
        p.getFirstrecordBody(orderWith);
        // String output = p.getFirstrecordBody(TestInfo.ORDER_FOR_TESTING_WARCINFO);
        // System.out.println(output);
    }

    @Test
    public void testWriteWarcInfoWithoutScheduleName() {
        // Change disk-path of order.xml to DirWithout
        Document doc = XmlUtils.getXmlDoc(orderWithOut);

        XmlUtils.setNode(doc, DISK_PATH_XPATH, orderWithOut.getParentFile().getAbsolutePath());
        XmlUtils.writeXmlToFile(doc, orderWithOut);

        WARCWriterProcessor p = new WARCWriterProcessor("testing");
        p.getFirstrecordBody(orderWithOut);
        // String output = p.getFirstrecordBody(TestInfo.ORDER_FOR_TESTING_WARCINFO);
        // System.out.println(output);
    }
}