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
package dk.netarkivet.harvester.datamodel;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.dom4j.Document;
import org.dom4j.Node;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.IllegalState;
import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.common.utils.IteratorUtils;
import dk.netarkivet.common.utils.RememberNotifications;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.SlowTest;
import dk.netarkivet.harvester.scheduler.jobgen.DefaultJobGenerator;
import dk.netarkivet.harvester.test.utils.OrderXmlBuilder;
import dk.netarkivet.harvester.webinterface.DomainDefinition;
import dk.netarkivet.harvester.webinterface.HarvestStatusQuery;
import dk.netarkivet.harvester.webinterface.HarvestStatusTester;

/**
 * Unit tests for the JobDAO class.
 */
@Category(SlowTest.class)
public class JobDAOTester extends DataModelTestCase {

    /** We start out with one job in status DONE. */
    private static final int INITIAL_JOB_COUNT = 1;

    private static final HarvestChannel FOCUSED_CHANNEL = new HarvestChannel("FOCUSED", false, true, "");
    private static final HarvestChannel SNAPSHOT_CHANNEL = new HarvestChannel("SNAPSHOT", true, true, "");
    private JobDAO jobDAO;
    private DomainConfiguration domainConfiguration;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        HarvestDAOUtils.resetDAOs();
        jobDAO = JobDAO.getInstance();

        domainConfiguration = TestInfo.getDRConfiguration();
        try {
            addHarvestDefinitionToDatabaseWithId(TestInfo.HARVESTID);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Category(SlowTest.class)
    @Test
    public void testGetCountJobs() throws Exception {
        assertEquals("Must have " + INITIAL_JOB_COUNT + " jobs from the" + " beginning", INITIAL_JOB_COUNT,
                jobDAO.getCountJobs());
        HarvestDefinitionDAO hdDao = HarvestDefinitionDAO.getInstance();
        HarvestDefinition hd = hdDao.read(Long.valueOf(42));
        DefaultJobGenerator jobGen = new DefaultJobGenerator();
        int jobsMade = jobGen.generateJobs(hd);
        assertEquals("Must find same number of jobs as we created", jobsMade + INITIAL_JOB_COUNT, jobDAO.getCountJobs());
        jobsMade = jobGen.generateJobs(hd);
        assertEquals("Must find all the jobs we have created", 2 * jobsMade + INITIAL_JOB_COUNT, jobDAO.getCountJobs());
    }

    /**
     * This test creates (and stores) a new job and reads it back again.
     * Verifies that state of stored job equals state of original job
     */
    @Test
    public void testJobRead() {
        Job job = createJob(0);
        jobDAO.create(job);

        Job readJob = jobDAO.read(job.getJobID());
        assertEquals("Id of read Job should equal id of original Job", job.getJobID(), readJob.getJobID());
        assertEquals("Status of read Job should equal status of original Job", job.getStatus(), readJob.getStatus());
        assertEquals("Seedlist of read Job should equal seedlist of original " + "Job", job.getSeedListAsString(),
                readJob.getSeedListAsString());
        assertEquals("Order.xml of read Job should equal order.xml of " + "original Job", job.getOrderXMLdoc()
                .getText(), readJob.getOrderXMLdoc().getText());
        assertEquals("Filename of order.xml of read Job should equal filename" + " of order.xml of original Job",
                job.getOrderXMLName(), readJob.getOrderXMLName());
        assertArrayEquals(
                "List of settings.xml's of read Job should equal list of" + " settings.xml's of original Job",
                job.getSettingsXMLdocs(), readJob.getSettingsXMLdocs());
        assertEquals("OrigHarvestDefinitionID of read Job should equal" + " OrigHarvestDefinitionID of original Job",
                job.getOrigHarvestDefinitionID(), readJob.getOrigHarvestDefinitionID());

        assertEquals("DomainConfigurationMap of read Job should equal" + " DomainConfigurationMap of original Job",
                job.getDomainConfigurationMap(), readJob.getDomainConfigurationMap());

        assertEquals("harvestnamePrefix of read Job should equal" + " harvestnamePrefix of original Job",
                job.getHarvestFilenamePrefix(), readJob.getHarvestFilenamePrefix());

        String defaultNamePrefix = "2-5678";
        assertEquals("harvestnamePrefix of read Job should equal" + " '2-5678'", defaultNamePrefix,
                readJob.getHarvestFilenamePrefix());
        readJob = jobDAO.read(job.getJobID());

        final String harvestnamePrefix = "netarkivet-collection";
        readJob.setHarvestFilenamePrefix(harvestnamePrefix);
        jobDAO.update(readJob);
        readJob = jobDAO.read(job.getJobID());
        assertEquals(
                "harvestname_prefix should be 'netarkivet-collection' but was' " + readJob.getHarvestFilenamePrefix(),
                harvestnamePrefix, readJob.getHarvestFilenamePrefix());

        // Job.getSettingsXMLfiles() is probably obsolete
        // No decided if we need Job.getActualStart() and Job.getActualStop()
        // - but we probably do (at least nice to have)
    }

    @Test(expected = UnknownID.class)
    public void testJobReadUnknownID() {
        jobDAO.read(42424242);
    }

    @Test(expected = UnknownID.class)
    public void testCreateJobWithUnknownHarvestId() {
        final Long UNKNOWN_HARVESTID = new Long(5679);
        Job job = new Job(UNKNOWN_HARVESTID, domainConfiguration, OrderXmlBuilder.createDefault().getOrderXml(),
                FOCUSED_CHANNEL, Constants.HERITRIX_MAXOBJECTS_INFINITY,
                Constants.HERITRIX_MAXBYTES_INFINITY, Constants.HERITRIX_MAXJOBRUNNINGTIME_INFINITY, 0);
        jobDAO.create(job);
    }

    @Test
    public void testJobUpdate() throws SQLException {
        Job job = createJob(0);
        DomainConfiguration firstConfiguration = TestInfo.getDRConfiguration();
        jobDAO.create(job);

        /* Modify the job and update */
        job.setStatus(JobStatus.DONE);
        DomainConfiguration anotherConfiguration = TestInfo.getNetarkivetConfiguration();
        job.addConfiguration(anotherConfiguration);
        jobDAO.update(job);

        // check that the modified job can be retrieved
        JobDAO jobDAO2 = JobDAO.getInstance();
        Job jobUpdated = jobDAO2.read(job.getJobID());

        assertTrue(
                "The retrieved job should have status " + JobStatus.DONE + ", but has status " + jobUpdated.getStatus(),
                jobUpdated.getStatus() == JobStatus.DONE);

        Map<String, String> domainConfigurationMap = jobUpdated.getDomainConfigurationMap();

        assertTrue("The DomainConfigurationMap of the retrieved job does not "
                   + "match that of the original job - domain name " + firstConfiguration.getDomainName() + " not found",
                domainConfigurationMap.containsKey(firstConfiguration.getDomainName()));
        assertTrue(
                "The DomainConfigurationMap of the retrieved job does not "
                + "match that of the original job - domain name " + anotherConfiguration.getDomainName()
                + " not found", domainConfigurationMap.containsKey(anotherConfiguration.getDomainName()));

        assertEquals("The DomainConfigurationMap of the retrieved job does not "
                     + "match that of the original job - domainConfiguration name " + firstConfiguration.getName()
                     + " not found",
                domainConfigurationMap.get(firstConfiguration.getDomainName()), firstConfiguration.getName());

        assertEquals("The DomainConfigurationMap of the retrieved job does not "
                     + "match that of the original job - domainConfiguration name " + anotherConfiguration.getName()
                     + " not found", domainConfigurationMap.get(anotherConfiguration.getDomainName()),
                anotherConfiguration.getName());
    }

    @Test(expected = UnknownID.class)
    public void testJobUpdateUnknownID() {
        Long unknownID = new Long(42424242);
        Job jobUknownID = createJob(0);
        jobUknownID.setJobID(unknownID);
        jobDAO.update(jobUknownID);
    }

    @Test(expected = ArgumentNotValid.class)
    public void testJobUpdateNullID() {
        jobDAO.update(null);
    }

    /**
     * Test that the max objects per domain attribute can be updated in persistent storage.
     */
    @Test
    public void testJobUpdateForceMaxObjectsPerDomain() throws Exception {
        Job job = new Job(TestInfo.HARVESTID, domainConfiguration, OrderXmlBuilder.createDefault().getOrderXml(),
                FOCUSED_CHANNEL, TestInfo.MAX_OBJECTS_PER_DOMAIN,
                Constants.HERITRIX_MAXBYTES_INFINITY, Constants.DEFAULT_MAX_JOB_RUNNING_TIME, 0);
        jobDAO.create(job);

        // check that the modified job can be retrieved
        JobDAO jobDAO2 = JobDAO.getInstance();
        Job jobUpdated = jobDAO2.read(job.getJobID());

        long expectedCappedMaxObjects = domainConfiguration.getMaxObjects();
        assertEquals("The retrieved job should have max object per domain = " + expectedCappedMaxObjects
                     + ", but it is equal to " + jobUpdated.getForceMaxObjectsPerDomain(), expectedCappedMaxObjects,
                jobUpdated.getForceMaxObjectsPerDomain());

        // check that the job-specific order.xml is modified accordingly:

        final Document orderXMLdoc = jobUpdated.getOrderXMLdoc();
        String xpath = "/crawl-order/controller/map[@name='pre-fetch-processors']"
                       + "/newObject[@name='QuotaEnforcer']" + "/long[@name='group-max-fetch-successes']";
        Node queueTotalBudgetNode = orderXMLdoc.selectSingleNode(xpath);
        assertEquals("OrderXML value should equals set value", expectedCappedMaxObjects,
                Integer.parseInt(queueTotalBudgetNode.getText()));
    }

    /*
     * Check that the correct number of jobs of various statuses are found with getAll()
     */
    private void assertJobsFound(String msg, int c_new, int c_submitted, int c_started, int c_failed, int c_done) {
        assertEquals(c_new + " jobs with status NEW should be present " + msg, c_new,
                IteratorUtils.toList(jobDAO.getAll(JobStatus.NEW)).size());
        assertEquals(c_started + " jobs with status STARTED should be present " + msg, c_started,
                IteratorUtils.toList(jobDAO.getAll(JobStatus.STARTED)).size());
        assertEquals(c_submitted + " jobs with status SUBMITTED should be present " + msg, c_submitted, IteratorUtils
                .toList(jobDAO.getAll(JobStatus.SUBMITTED)).size());
        assertEquals(c_failed + " jobs with status FAILED should be present " + msg, c_failed,
                IteratorUtils.toList(jobDAO.getAll(JobStatus.FAILED)).size());
        assertEquals((INITIAL_JOB_COUNT + c_done) + " jobs with status DONE should be present " + msg,
                INITIAL_JOB_COUNT + c_done, IteratorUtils.toList(jobDAO.getAll(JobStatus.DONE)).size());
    }

    /**
     * Test getting jobs with various statuses
     */
    @Test
    public void testGetAll() throws Exception {
        assertJobsFound("at start", 0, 0, 0, 0, 0);
        int num_jobs = 5;
        List<Job> jobs = new ArrayList<Job>(num_jobs);
        for (int i = 0; i < num_jobs; i++) {
            Job j = createJob(i);//Job.createJob(Long.valueOf(42), FOCUSED_CHANNEL, cfg, 0);
            jobDAO.create(j);
            jobs.add(j);
        }
        // Check that they all exist
        assertJobsFound("after adding jobs", num_jobs, 0, 0, 0, 0);
        setJobStatus(jobs, 0, JobStatus.NEW);
        setJobStatus(jobs, 1, JobStatus.STARTED);
        setJobStatus(jobs, 2, JobStatus.SUBMITTED);
        setJobStatus(jobs, 3, JobStatus.DONE);
        setJobStatus(jobs, 4, JobStatus.FAILED);
        assertJobsFound("after setting one of each", 1, 1, 1, 1, 1);
        setJobStatus(jobs, 0, JobStatus.STARTED);
        setJobStatus(jobs, 1, JobStatus.STARTED);
        setJobStatus(jobs, 2, JobStatus.FAILED);
        setJobStatus(jobs, 3, JobStatus.FAILED);
        setJobStatus(jobs, 4, JobStatus.FAILED);
        assertJobsFound("only started and failed jobs", 0, 0, 2, 3, 0);
    }

    @Test
    public void testPersistensOfHarvestChannel() throws SQLException {
        Job job0 = createJob(0);
        assertEquals("The channel should be named highpriority", "FOCUSED", job0.getChannel());
        Job job1 = createJob(1);
        job1.setHarvestChannel(SNAPSHOT_CHANNEL);
        assertEquals("The channel should be named " + SNAPSHOT_CHANNEL.getName(),
                SNAPSHOT_CHANNEL.getName(), job1.getChannel());

        jobDAO.create(job0);
        jobDAO.create(job1);

        // read them again
        Job job2 = jobDAO.read(job0.getJobID());
        Job job3 = jobDAO.read(job1.getJobID());
        assertEquals("Jobs should preserve channel", job0.getChannel(), job2.getChannel());
        assertEquals("Jobs should preserve channel", job1.getChannel(), job3.getChannel());
    }

    /**
     * Verifies the functionality of the #getAllJobIds(JobStatus, HarvestChannel)
     */
    @Test
    public void testGetAllJobIdsForStatusAndChannel() throws SQLException {
        Iterator<Long> idsForFocusedJobs = jobDAO.getAllJobIds(JobStatus.NEW, FOCUSED_CHANNEL);
        assertTrue("Initiel size of jobs with jobstatus " + JobStatus.NEW + " and channel FOCUSED_CHANNEL"
                   + " larger than zero", !idsForFocusedJobs.hasNext());

        Iterator<Long> idsForSnapshotJobs = jobDAO.getAllJobIds(JobStatus.NEW, SNAPSHOT_CHANNEL);
        assertTrue("Initiel size of jobs with jobstatus " + JobStatus.NEW + " and channel SNAPSHOT_CHANNEL"
                   + " larger than zero", !idsForSnapshotJobs.hasNext());

        // Create a high and a low priority job
        Job focusedJobID = createJob(0);
        Job snapshotJobID = createJob(1);
        snapshotJobID.setHarvestChannel(SNAPSHOT_CHANNEL);
        jobDAO.create(focusedJobID);
        jobDAO.create(snapshotJobID);

        idsForFocusedJobs = jobDAO.getAllJobIds(JobStatus.NEW, FOCUSED_CHANNEL);
        assertTrue("No job with jobstatus " + JobStatus.NEW + " and channel HIGHPRIORITY"
                   + " returned after creating job", idsForFocusedJobs.hasNext());
        Job snapshotJob = jobDAO.read(idsForFocusedJobs.next());
        assertEquals("Job should have high priority", focusedJobID.getChannel(), snapshotJob.getChannel());

        idsForSnapshotJobs = jobDAO.getAllJobIds(JobStatus.NEW, SNAPSHOT_CHANNEL);
        assertTrue("No job with jobstatus " + JobStatus.NEW + " and channel SNAPSHOT_CHANNEL"
                   + " returned after creating job", jobDAO.getAllJobIds(JobStatus.NEW, SNAPSHOT_CHANNEL).hasNext());
        Job jobLowPriority = jobDAO.read(idsForSnapshotJobs.next());
        assertEquals("Job should have low priority", snapshotJobID.getChannel(), jobLowPriority.getChannel());
    }

    private void setJobStatus(List<Job> jobs, int i, JobStatus status) {
        jobs.get(i).setStatus(status);
        jobDAO.update((jobs.get(i)));
    }

    /** Test that the job error info is stored correctly. */
    @Test
    public void testPersistenceOfJobErrors() throws Exception {
        Job j = createJob(1);
        jobDAO.create(j);
        Job j2 = jobDAO.read(j.getJobID());
        assertNull("Should have no harvest error by default", j2.getHarvestErrors());
        assertNull("Should have no harvest error details by default", j2.getHarvestErrorDetails());
        assertNull("Should have no upload error by default", j2.getUploadErrors());
        assertNull("Should have no upload error details by default", j2.getUploadErrorDetails());
        j2.appendHarvestErrors("str1");
        j2.appendHarvestErrorDetails("str2");
        j2.appendUploadErrors("str3");
        j2.appendUploadErrorDetails("str4");
        jobDAO.update(j2);
        Job j3 = jobDAO.read(j2.getJobID());
        assertEquals("Should have new harvest error string", "str1", j3.getHarvestErrors());
        assertEquals("Should have new harvest error detail string", "str2", j3.getHarvestErrorDetails());
        assertEquals("Should have new upload error string", "str3", j3.getUploadErrors());
        assertEquals("Should have new upload error detail string", "str4", j3.getUploadErrorDetails());
    }

    /**
     * Reset the job jobDAO.
     */
    public static void resetDAO() {
        JobDAO.reset();
    }

    /**
     * Test that we can get reasonable status info about jobs.
     */
    @Test
    public void failingTestGetStatusInfo() throws Exception {
        Job job1 = jobDAO.read(1L);
        List<JobStatusInfo> infos = jobDAO.getStatusInfo(new HarvestStatusQuery()).getJobStatusInfo();
        assertEquals("Should get info for one job initially", 1, infos.size());
        checkInfoCorrect(job1, infos.get(0));

        Job job2 = createJob(1);
        jobDAO.create(job2);
        job2.appendUploadErrors("Bad stuff");
        job2.appendHarvestErrors("Good harvest");
        job2.setActualStart(new Date());
        jobDAO.update(job2);

        infos = jobDAO.getStatusInfo(new HarvestStatusQuery()).getJobStatusInfo();
        assertEquals("Should get info on two jobs", 2, infos.size());
        Map<JobStatus, JobStatusInfo> jobStatusSet = new HashMap<>();
        for (JobStatusInfo info : infos) {
            jobStatusSet.put(info.getStatus(), info);
        }
        checkInfoCorrect(job1, jobStatusSet.get(JobStatus.DONE));
        checkInfoCorrect(job2, jobStatusSet.get(JobStatus.NEW));

        Map<String, String[]> params = new HashMap<>();
        params.put(HarvestStatusQuery.UI_FIELD.JOB_ID_ORDER.name(),
                new String[] {HarvestStatusQuery.SORT_ORDER.DESC.name()});
        params.put(HarvestStatusQuery.UI_FIELD.JOB_STATUS.name(),
                new String[] {job2.getStatus().name()});
        HarvestStatusQuery query = HarvestStatusTester.getTestQuery(params);
        infos = jobDAO.getStatusInfo(query).getJobStatusInfo();
        assertEquals("Query returned wrong number of jobs", 1, infos.size());
        assertThat("JobID of new Job", infos.get(0).getJobID(), equalTo(job2.getJobID()));
        checkInfoCorrect(job2, infos.get(0));
    }

    /**
     * Test that we can get reasonable status info about jobs from specific harvest runs.
     */
    @Test
    public void testGetStatusInfoForHarvest() throws Exception {
        Job job2 = createJob(3);
        jobDAO.create(job2);
        Job job3 = createJob(4);
        jobDAO.create(job3);
        Job job4 = createJob(4);
        jobDAO.create(job4);

        List<JobStatusInfo> infos = jobDAO.getStatusInfo(new HarvestStatusQuery(43L, 0)).getJobStatusInfo();
        assertEquals("Should get info on no jobs", 0, infos.size());

        infos = jobDAO.getStatusInfo(new HarvestStatusQuery(117L, 23)).getJobStatusInfo();
        assertEquals("Should get info on no jobs", 0, infos.size());

        infos = jobDAO.getStatusInfo(new HarvestStatusQuery(job2.getOrigHarvestDefinitionID(), 3)).getJobStatusInfo();
        assertEquals("Should get info on one job", 1, infos.size());
        JobStatusInfo info = infos.get(0);
        checkInfoCorrect(job2, info);

        infos = jobDAO.getStatusInfo(new HarvestStatusQuery(job3.getOrigHarvestDefinitionID(), 4)).getJobStatusInfo();
        assertEquals("Should get info on two jobs", 2, infos.size());
        info = infos.get(0);
        checkInfoCorrect(job3, info);
        info = infos.get(1);
        checkInfoCorrect(job4, info);
    }

    private void checkInfoCorrect(Job j, JobStatusInfo info) {
        HarvestDefinitionDAO hddao = HarvestDefinitionDAO.getInstance();
        assertEquals("Info should be for job " + j.getJobID(), j.getJobID(), j.getJobID());
        assertEquals("Status for job " + j.getJobID(), j.getStatus(), info.getStatus());
        assertEquals("HarvestID for job " + j.getJobID(), (long) j.getOrigHarvestDefinitionID(),
                info.getHarvestDefinitionID());
        assertEquals("HarvestNum for job " + j.getJobID(), j.getHarvestNum(), info.getHarvestNum());
        assertEquals("HarvestName for job " + j.getJobID(), hddao.read(j.getOrigHarvestDefinitionID()).getName(),
                info.getHarvestDefinition());
        assertEquals("HarvestError for job " + j.getJobID(), j.getHarvestErrors(), info.getHarvestErrors());
        assertEquals("UploadError for job " + j.getJobID(), j.getUploadErrors(), info.getUploadErrors());
        assertEquals("OrderXML name for job  " + j.getJobID(), j.getOrderXMLName(), info.getOrderXMLname());
        assertEquals("Domain count for job " + j.getJobID(), j.getDomainConfigurationMap().size(),
                info.getConfigCount());
        assertEquals("Start date for job " + j.getJobID(), j.getActualStart(), info.getStartDate());
        assertEquals("End date for job " + j.getJobID(), j.getActualStop(), info.getEndDate());
    }

    /** Check that start and end dates are created and stored correctly. */
    @Test
    public void testSetDates() {
        DomainDAO ddao = DomainDAO.getInstance();
        Domain d = ddao.read("netarkivet.dk");
        Date startDate = new Date();
        Job newJob1 = createJob(2);
        newJob1.setStatus(JobStatus.SUBMITTED);
        jobDAO.create(newJob1);
        assertNull("Should have null start date at start, but was " + newJob1.getActualStart(),
                newJob1.getActualStart());
        assertNull("Should have null stop date at start, but was " + newJob1.getActualStop(), newJob1.getActualStop());
        newJob1.setStatus(JobStatus.STARTED);
        assertNotNull("Should have non-null start date after starting", newJob1.getActualStart());
        assertFalse("Should have updated start date after starting (>= before)",
                startDate.after(newJob1.getActualStart()));
        assertFalse("Should have updated start date after starting (<= now)",
                new Date().before(newJob1.getActualStart()));
        assertNull("Should have null stop date after starting, but was " + newJob1.getActualStop(),
                newJob1.getActualStop());
        jobDAO.update(newJob1);
        Job newJob2 = jobDAO.read(newJob1.getJobID());
        assertNotNull("Should have non-null start date after rereading", newJob2.getActualStart());
        assertEquals("Should have same start date after rereading", newJob1.getActualStart(), newJob2.getActualStart());
        assertNull("Should have null stop date after rereading, but was " + newJob2.getActualStop(),
                newJob2.getActualStop());
        try {
            // Make sure new time is different
            Thread.sleep(1);
        } catch (InterruptedException e) {
            // Ignored
        }
        Date stopDate = new Date();
        newJob2.setStatus(JobStatus.DONE);
        assertNotNull("Should have non-null start date after finishing", newJob2.getActualStart());
        assertEquals("Should have same start date after rereading", newJob1.getActualStart(), newJob2.getActualStart());
        assertNotNull("Should have non-null stop date after finishing", newJob2.getActualStop());
        assertFalse("Should have updated stop date after finishing (>= before)",
                stopDate.after(newJob2.getActualStop()));
        assertFalse("Should have updated stop date after finishing (<= now)",
                new Date().before(newJob2.getActualStop()));
        jobDAO.update(newJob2);
        Job newJob3 = jobDAO.read(newJob2.getJobID());
        assertNotNull("Should have non-null start date after rerereading", newJob3.getActualStart());
        assertEquals("Should have same start date after rereading", newJob2.getActualStart(), newJob3.getActualStart());
        assertNotNull("Should have non-null stop date after rerereading", newJob3.getActualStop());
        assertEquals("Should have same stop date after rereading", newJob2.getActualStop(), newJob3.getActualStop());
        // Also check that you can't mess with the dates.
        try {
            // Make sure new time is different
            Thread.sleep(1);
        } catch (InterruptedException e) {
            // Ignored
        }

        // It is now possible to set start date to after end date '
        // without any exceptions being thrown. However, a notification is
        // emitted.
        Settings.set(CommonSettings.NOTIFICATIONS_CLASS, RememberNotifications.class.getName());
        RememberNotifications.resetSingleton();

        try {
            newJob3.setActualStart(new Date());
            assertTrue("Setting start date to after end date should result " + "in notification",
                    RememberNotifications.getInstance().message.length() > 0);
        } catch (ArgumentNotValid e) {
            fail("Setting start date to after end date should not throw " + "exception: " + e);
        }
        RememberNotifications.resetSingleton();
        newJob3.setActualStart(stopDate);

        try {
            newJob3.setActualStop(startDate);
            assertTrue("Setting stop date to before end date should result in" + " notification",
                    RememberNotifications.getInstance().message.length() > 0);
        } catch (ArgumentNotValid e) {
            fail("Setting stop date to before end date should not throw " + "exception: " + e);
        }
    }

    /**
     * Tests the retrieval of jobs to use for duplicate reduction.
     * <p>
     * The following cases are tested:
     * <p>
     * Unknown job ID. Partial harvests with no previous harvest is present should return empty list. Partial harvests
     * with two previous harvests should return jobs from first harvest. Full harvest with no previous full harvests
     * should return empty list. Full harvest not based on anything but with previous chain should return that. Full
     * harvest based on something but with no previous chain should return that. Full harvest based on something AND
     * with previous chains should return that.
     */
    @Test
    public void testGetJobIDsForDuplicateReduction() throws Exception {
        // Assume 1st job has id=2, and Last job has id 15
        createTestJobs(2L, 15L);

        try {
            jobDAO.getJobIDsForDuplicateReduction(9999L);
            fail("Expected UnknownID on job ID not in database");
        } catch (UnknownID e) {
            // expected
        }

        List<Long> result;
        List<Long> expected;

        result = jobDAO.getJobIDsForDuplicateReduction(2L);
        assertEquals("Should get empty list on no previous harvest", 0, result.size());

        result = jobDAO.getJobIDsForDuplicateReduction(7L);
        expected = Arrays.asList(new Long[] {4L, 5L});
        Collections.sort(result);
        Collections.sort(expected);
        assertEquals("Should get previous harvests' job ids in list", expected, result);

        result = jobDAO.getJobIDsForDuplicateReduction(8L);
        assertEquals("Should get empty list on no previous harvest", 0, result.size());

        result = jobDAO.getJobIDsForDuplicateReduction(10L);
        expected = Arrays.asList(new Long[] {8L, 9L});
        Collections.sort(result);
        Collections.sort(expected);
        assertEquals("Should get originating harvests' job ids in list", expected, result);

        result = jobDAO.getJobIDsForDuplicateReduction(12L);
        expected = Arrays.asList(new Long[] {8L, 9L, 10L, 11L});
        Collections.sort(result);
        Collections.sort(expected);
        assertEquals("Should get previous full harvests' job ids in list", expected, result);

        result = jobDAO.getJobIDsForDuplicateReduction(14L);
        expected = Arrays.asList(new Long[] {8L, 9L, 10L, 11L, 12L, 13L});
        Collections.sort(result);
        Collections.sort(expected);
        assertEquals("Should get previous full harvests' job ids in list", expected, result);
    }

    private void compareCopiedJob(Job oldJob1, Job newJob1, Long newID) {
        assertEquals("Should have same domain count", oldJob1.getCountDomains(), newJob1.getCountDomains());
        assertEquals("Should have same domain config map", oldJob1.getDomainConfigurationMap(),
                newJob1.getDomainConfigurationMap());
        assertEquals("Should have same forceMaxObjects", oldJob1.getForceMaxObjectsPerDomain(),
                newJob1.getForceMaxObjectsPerDomain());
        assertEquals("Should have same max bytes", oldJob1.getMaxBytesPerDomain(), newJob1.getMaxBytesPerDomain());
        assertEquals("Should have same max objects", oldJob1.getMaxObjectsPerDomain(), newJob1.getMaxObjectsPerDomain());
        assertEquals("Should have same order.xml", oldJob1.getOrderXMLdoc().asXML(), newJob1.getOrderXMLdoc().asXML());
        assertEquals("Should have same order xml name", oldJob1.getOrderXMLName(), newJob1.getOrderXMLName());
        assertEquals("Should have same original harvest id", oldJob1.getOrigHarvestDefinitionID(),
                newJob1.getOrigHarvestDefinitionID());
        assertEquals("Should have same channel", oldJob1.getChannel(), newJob1.getChannel());
        assertEquals("Should have same seedlist", oldJob1.getSeedListAsString(), newJob1.getSeedListAsString());
        assertArrayEquals("Should have same settingsxml docs", oldJob1.getSettingsXMLdocs(),
                newJob1.getSettingsXMLdocs());
        assertArrayEquals("Should have same settingsxml files", oldJob1.getSettingsXMLfiles(),
                newJob1.getSettingsXMLfiles());
        assertEquals("Should have new status", JobStatus.NEW, newJob1.getStatus());
        // TODO changed from 2L to 1L
        assertEquals("Should have new edition", 1L, newJob1.getEdition());
        assertEquals("Should have new ID", newID, newJob1.getJobID());
        /*
         * assertNotSame("The harvestnamePrefixes should not be the same", oldJob1.getHarvestFilenamePrefix(),
         * newJob1.getHarvestFilenamePrefix());
         */
    }

    public static void changeStatus(long jobID, JobStatus newStatus) {
        PreparedStatement s = null;
        Connection c = HarvestDBConnection.get();
        try {
            s = c.prepareStatement("update jobs set status=? where job_id=?");
            s.setLong(1, newStatus.ordinal());
            s.setLong(2, jobID);
            s.executeUpdate();
        } catch (SQLException e) {
            String message = "SQL error changing job state for job with id=" + jobID + " in database";
            throw new IOFailure(message, e);
        } finally {
            HarvestDBConnection.release(c);
        }
    }

    /**
     * Tests method in JobDBDAO.rescheduleJob Now verifies, that the new job has startdate and enddate set to null.
     */
    @Test
    public void testRescheduleJob() {
        // Assume 1st job has id=2, and Last job has id 15
        createTestJobs(2L, 15L);

        for (long i = 1; i < 16; i++) {
            Job oldJob = jobDAO.read(i);
            if (oldJob.getStatus() != JobStatus.SUBMITTED && oldJob.getStatus() != JobStatus.FAILED) {
                try {
                    jobDAO.rescheduleJob(i);
                    fail("Should not have been able to resubmit job " + oldJob);
                } catch (IllegalState e) {
                    // expected;
                }
            }
        }

        for (long i = 1; i < 16; i++) {
            changeStatus(i, i % 2 == 0 ? JobStatus.SUBMITTED : JobStatus.FAILED);
            long newJobID = jobDAO.rescheduleJob(i);
            Job oldJob = jobDAO.read(i);
            Job newJob = jobDAO.read(newJobID);
            long newID = i + 15;
            compareCopiedJob(oldJob, newJob, newID);
            assertEquals("Old job should have resubmitted status", JobStatus.RESUBMITTED, oldJob.getStatus());
            assertTrue("New job must have null startdate", newJob.getActualStart() == null);
            assertTrue("New job must have null enddate", newJob.getActualStop() == null);
        }

        try {
            jobDAO.rescheduleJob(42L);
            fail("Should not have been able to resubmit non-existing job");
        } catch (UnknownID e) {
            // expected
        }
    }

    @Test
    public void testgetJobAliasInfo() {
        DomainConfiguration dc = TestInfo.getDefaultConfig(TestInfo.getDefaultDomain());
        Job job = createJob(0);
        DomainConfiguration anotherConfig = TestInfo.getConfigurationNotDefault(TestInfo.getDomainNotDefault());
        job.addConfiguration(anotherConfig);
        // domains in job: job.getDomainConfigurationMap().keySet();
        DomainDAO ddao = DomainDAO.getInstance();
        List<AliasInfo> aliases = jobDAO.getJobAliasInfo(job);
        // aliases equals #domains being skipped because a domain in job.getDomainConfigurationMap().keySet()
        // is the aliasFather for that domain
        assertTrue("No domains are skipped, as no aliases are defined", aliases.isEmpty());
        DomainDefinition.createDomains("alias1.dk", "alias2.dk", "alias3.dk");
        Domain d = ddao.read("kb.dk");
        DomainConfiguration dc1 = TestInfo.getConfig(d, "aliasKonfig");
        d = ddao.read("dr.dk");
        DomainConfiguration dc2 = TestInfo.getConfig(d, "aliasKonfig2");
        d = ddao.read("alias1.dk");
        d.updateAlias("kb.dk");
        ddao.update(d);
        d = ddao.read("alias2.dk");
        d.updateAlias("kb.dk");
        ddao.update(d);
        d = ddao.read("alias3.dk");
        d.updateAlias("dr.dk");
        ddao.update(d);
        job = createJob(1);
        job.addConfiguration(dc1);
        job.addConfiguration(dc2);
        // this should give us a List of size 3:
        aliases = jobDAO.getJobAliasInfo(job);
        assertEquals("There should be 3 AliasInfo objects in the List returned", 3, aliases.size());
    }

    @Test
    public void testMaxBytesBug652() throws Exception {
        DomainConfiguration defaultConfig = DomainConfigurationTester.createDefaultDomainConfiguration();
        defaultConfig.setMaxBytes(-1);

        Job job =  new Job(TestInfo.HARVESTID, defaultConfig, OrderXmlBuilder.createDefault().getOrderXml(),
                FOCUSED_CHANNEL, Constants.HERITRIX_MAXOBJECTS_INFINITY,
                Constants.HERITRIX_MAXBYTES_INFINITY, Constants.HERITRIX_MAXJOBRUNNINGTIME_INFINITY, 0);;
        // test default value of forceMaxObjectsPerDomain:
        assertEquals("No limit of value of forceMaxObjectsPerDomain expected", -1, job.getMaxBytesPerDomain());
        JobDAO jDao = JobDAO.getInstance();
        jDao.create(job); // save job in Database.
        Iterator<Job> jobIterator = jDao.getAll();
        while (jobIterator.hasNext()) {
            Job j1 = jobIterator.next();
            if (j1.getMaxBytesPerDomain() == 1) {
                fail("Maxbytes (-1) stored as (1)");
            }
        }
    }

    private Job createJob(int harvestNum) {
        return new Job(TestInfo.HARVESTID, domainConfiguration, OrderXmlBuilder.createDefault().getOrderXml(),
                FOCUSED_CHANNEL, Constants.HERITRIX_MAXOBJECTS_INFINITY,
                Constants.HERITRIX_MAXBYTES_INFINITY, Constants.HERITRIX_MAXJOBRUNNINGTIME_INFINITY, harvestNum);
    }
}

