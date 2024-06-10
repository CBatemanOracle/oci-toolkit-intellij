package com.oracle.oci.intellij.ui.appstack.command;

import com.oracle.bmc.resourcemanager.model.JobSummary;
import com.oracle.bmc.resourcemanager.model.StackSummary;
import com.oracle.oci.intellij.account.OracleCloudAccount;
import com.oracle.oci.intellij.common.command.AbstractBasicCommand;
import com.oracle.oci.intellij.ui.appstack.models.proxies.StackProxy;
import com.oracle.oci.intellij.ui.common.cache.SimpleCache;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
/**
 * A command class that retrieves detailed stack information from Oracle Cloud Infrastructure, including associated jobs,
 * and encapsulates this information in a list of {@link StackProxy} objects. This class extends the {@link AbstractBasicCommand}
 * to leverage its structured command execution pattern for consistent error handling and result encapsulation.
 *
 * Unlike {@link ListStackCommand}, which only retrieves and returns stack summaries, this class provides a comprehensive
 * view by also fetching the jobs associated with each stack. It then encapsulates both stacks and their corresponding job
 * summaries into {@link StackProxy} objects, providing a unified data model that simplifies further processing and usage
 * within the application.
 */
public class ListStackProxyCommand extends AbstractBasicCommand<ListStackProxyCommand.ListStackProxyResult> {
    private OracleCloudAccount.ResourceManagerClientProxy resManagerClient;
    private String compartmentId;
    private SimpleCache<String,List<JobSummary>> jobsCache ;

    public static class ListStackProxyResult extends AbstractBasicCommand.Result {

        private List<StackProxy> stacks;

        public ListStackProxyResult(Severity severity, Status status, List<StackProxy> stacks) {
            super(severity, status);
            this.stacks = stacks;
        }

        public String toString() {
            final StringBuilder builder = new StringBuilder();
            Optional.ofNullable(this.stacks).ifPresent(stacks -> {

                stacks.forEach(stack -> builder.append(stack.toString()));
            });
            return builder.toString();
        }

        public List<StackProxy> getStacks() {
            return Collections.unmodifiableList(stacks);
        }

    }

    public ListStackProxyCommand(OracleCloudAccount.ResourceManagerClientProxy resManagerClient, String compartmentId,SimpleCache<String ,List<JobSummary>> jobsCache) {
        super();
        this.resManagerClient = resManagerClient;
        this.compartmentId = compartmentId;
        this.jobsCache = jobsCache;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected ListStackProxyResult doExecute() throws Exception {
        try {
            List<StackSummary> listOfAppStacks = resManagerClient.listStacks(compartmentId);
            List<StackProxy> stackProxies = getStackProxyList(listOfAppStacks,resManagerClient);

            return new ListStackProxyResult(AbstractBasicCommand.Result.Severity.NONE,
                    Result.Status.OK, stackProxies);
        }
        catch (com.oracle.bmc.model.BmcException bmcExcep) {
            if (bmcExcep.getStatusCode() == 404) {
                // not found or empty; pretend emtpy
                return new ListStackProxyResult(AbstractBasicCommand.Result.Severity.WARNING,
                        Result.Status.FAILED, Collections.EMPTY_LIST);
            }
            throw bmcExcep;
        }
    }

    private  List<StackProxy> getStackProxyList(List<StackSummary> listOfAppStacks, OracleCloudAccount.ResourceManagerClientProxy resManagerClient) {
        List<StackProxy> stackProxies = new ArrayList<>();
        listOfAppStacks.forEach(stackSummary -> {
            StackProxy sp = new StackProxy(stackSummary,resManagerClient,jobsCache);

            stackProxies.add(sp);
        });
        return stackProxies;
    }
}
