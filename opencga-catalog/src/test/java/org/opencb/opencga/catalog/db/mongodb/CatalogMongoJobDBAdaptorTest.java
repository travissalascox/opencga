package org.opencb.opencga.catalog.db.mongodb;

import org.junit.Test;
import org.opencb.commons.datastore.core.QueryResult;
import org.opencb.opencga.catalog.exceptions.CatalogDBException;
import org.opencb.opencga.catalog.models.Job;

import java.util.Collections;

import static org.junit.Assert.*;

/**
 * Created by pfurio on 3/2/16.
 */
public class CatalogMongoJobDBAdaptorTest extends CatalogMongoDBAdaptorTest {

    @Test
    public void createJobTest() throws CatalogDBException {
        Job job = new Job();
        job.setVisits(0);

        long studyId = user3.getProjects().get(0).getStudies().get(0).getId();
        job.setName("jobName1");
        System.out.println(catalogJobDBAdaptor.createJob(studyId, job, null));
//        long analysisId = catalogDBAdaptor.getAnalysisId(studyId, "analysis1Alias");

        job.setName("jobName2");
        System.out.println(catalogJobDBAdaptor.createJob(studyId, job, null));
        try {
            catalogJobDBAdaptor.createJob(-1, job, null);
            fail("error: expected exception");
        } catch (CatalogDBException e) {
            System.out.println("correct exception: " + e);
        }
    }

    @Test
    public void deleteJobTest() throws CatalogDBException {
        long studyId = user3.getProjects().get(0).getStudies().get(0).getId();

        Job job = catalogJobDBAdaptor.createJob(studyId, new Job("name", user3.getId(), "", "", "", 4, null, Collections.<Long>emptyList
                ()), null).first();
        long jobId = job.getId();
        assertEquals("active", job.getStatus().getStatus());
        QueryResult<Job> queryResult = catalogJobDBAdaptor.delete(jobId, false);
        System.out.println(queryResult);
        assertTrue(queryResult.getNumResults() == 1);
        assertEquals("deleted", queryResult.first().getStatus().getStatus());
        try {
            System.out.println(catalogJobDBAdaptor.delete(-1, false));
            fail("error: Expected \"Job not found\" exception");
        } catch (CatalogDBException e) {
            System.out.println("correct exception: " + e);
        }
    }

    @Test
    public void getAllJobTest() throws CatalogDBException {
        long studyId = user3.getProjects().get(0).getStudies().get(0).getId();
//        long analysisId = catalogDBAdaptor.getAnalysisId(studyId, "analysis1Alias");
        QueryResult<Job> allJobs = catalogJobDBAdaptor.getAllJobsInStudy(studyId, null);
        System.out.println(allJobs);
    }


    @Test
    public void getJobTest() throws CatalogDBException {
        long studyId = user3.getProjects().get(0).getStudies().get(0).getId();

        Job job = catalogJobDBAdaptor.createJob(studyId, new Job("name", user3.getId(), "", "", "", 4, null, Collections.<Long>emptyList
                ()), null).first();
        long jobId = job.getId();

        job = catalogJobDBAdaptor.getJob(jobId, null).first();
        System.out.println(job);

        try {
            catalogJobDBAdaptor.getJob(-1, null);
            fail("error: expected exception");
        } catch (CatalogDBException e) {
            System.out.println("correct exception: " + e);
        }

    }

    @Test
    public void incJobVisits() throws CatalogDBException {
        long studyId = user3.getProjects().get(0).getStudies().get(0).getId();
        Job jobBefore = catalogJobDBAdaptor.createJob(studyId, new Job("name", user3.getId(), "", "", "", 4, null, Collections
                .<Long>emptyList()), null).first();
        long jobId = jobBefore.getId();

        Long visits = (Long) catalogJobDBAdaptor.incJobVisits(jobBefore.getId()).first().get("visits");

        Job jobAfter = catalogJobDBAdaptor.getJob(jobId, null).first();
        assertTrue(jobBefore.getVisits() == jobAfter.getVisits() - 1);
        assertTrue(visits == jobAfter.getVisits());
    }

}
