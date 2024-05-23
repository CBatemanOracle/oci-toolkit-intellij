package com.oracle.oci.intellij.ui.appstack.command.common;
//
//import com.oracle.bmc.resourcemanager.model.JobSummary;
//import com.oracle.bmc.resourcemanager.model.StackSummary;
//import com.oracle.oci.intellij.account.OracleCloudAccount;
//import com.oracle.oci.intellij.ui.appstack.models.proxies.StackProxy;
//import org.jetbrains.annotations.NotNull;
//
//import java.util.ArrayList;
//import java.util.List;
//
//public class Util {
//    @NotNull
//    public static List<StackProxy> getStackProxyList(List<StackSummary> listOfAppStacks, OracleCloudAccount.ResourceManagerClientProxy resManagerClient) {
//        List<StackProxy> stackProxies = new ArrayList<>();
//        listOfAppStacks.forEach(stackSummary -> {
//            StackProxy sp = getStackProxy(resManagerClient, stackSummary);
//            stackProxies.add(sp);
//        });
//        return stackProxies;
//    }
//
//    @NotNull
//    public static StackProxy getStackProxy(OracleCloudAccount.ResourceManagerClientProxy resManagerClient, StackSummary stackSummary) {
//        List<JobSummary> jobSummaries = resManagerClient.listJobs(stackSummary.getCompartmentId(), stackSummary.getId()).getItems();
//
//        return new StackProxy(stackSummary,jobSummaries);
//    }
//}
