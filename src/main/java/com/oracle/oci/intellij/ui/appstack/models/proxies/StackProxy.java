package com.oracle.oci.intellij.ui.appstack.models.proxies;

import com.oracle.bmc.resourcemanager.model.JobSummary;
import com.oracle.bmc.resourcemanager.model.StackSummary;
import com.oracle.oci.intellij.account.OracleCloudAccount;
import com.oracle.oci.intellij.ui.common.cache.SimpleCache;

import java.util.Collections;
import java.util.List;

/**
 * Represents a proxy class that encapsulates details about a stack in Oracle Cloud Infrastructure.
 * It combines stack summary, and associated jobs into a single object.
 * This class provides a cohesive and convenient access point to all relevant data about a specific stack,
 * making it easier to manage and retrieve stack-related information without accessing multiple data sources.
 */
public class StackProxy {
    private StackSummary stackSummary;
    private List<JobSummary> jobs;
    private OracleCloudAccount.ResourceManagerClientProxy resManagerClient;
    private final SimpleCache<String , List<JobSummary>> jobsCache;

    public StackProxy(StackSummary stackSummary, OracleCloudAccount.ResourceManagerClientProxy resManagerClient, SimpleCache<String, List<JobSummary>> jobsCache) {
        this.stackSummary = stackSummary;
        this.resManagerClient = resManagerClient;
        this.jobsCache = jobsCache;
        jobs = fetchJobs();
    }

    public StackSummary getStackSummary() {
        return stackSummary;
    }

    public List<JobSummary> getJobs() {
        return Collections.unmodifiableList(jobs);
    }

    /**
     * Returns the most recent job from the list of jobs.
     * Assumes that the jobs are ordered such that the most recent job is at index 0.
     * If the list of jobs is empty, this method returns null.
     *
     * @return the most recent JobSummary, or null if there are no jobs.
     */
    public JobSummary getLastJob() {
        return jobs.isEmpty() ? null : jobs.get(0);
    }

    public List<JobSummary> fetchJobs() {
        if (jobsCache!= null){
            if (jobsCache.get(stackSummary.getId()) != null){
                return jobsCache.get(stackSummary.getId());
            }
            List <JobSummary> jobSummaries = resManagerClient.listJobs(stackSummary.getCompartmentId(),stackSummary.getId()).getItems();
            jobsCache.put(stackSummary.getId(),jobSummaries);
            return jobSummaries;
        }
        return null;
    }
}
