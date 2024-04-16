package com.oracle.oci.intellij.ui.common;

import java.util.function.Function;

import org.jetbrains.annotations.NotNull;

import com.intellij.notification.NotificationType;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.oracle.bmc.model.BmcException;
import com.oracle.bmc.resourcemanager.model.Job;
import com.oracle.bmc.resourcemanager.model.Job;
import com.oracle.oci.intellij.account.OracleCloudAccount;
import com.oracle.oci.intellij.common.command.BasicCommand;
import com.oracle.oci.intellij.ui.appstack.AppStackDashboard;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

import static com.oracle.bmc.resourcemanager.model.Job.LifecycleState.Failed;
import static com.oracle.bmc.resourcemanager.model.Job.LifecycleState.Succeeded;

public class MyBackgroundTask {
    private volatile boolean isRunning = true ;
    private  final Function<String,Boolean> isJobFinished=(jobId)->{
        try {
            while (isRunning) {
                Job job = getJob(jobId);
//                String status =job.getLifecycleState().getValue();
                if (Succeeded.equals(job.getLifecycleState())) {
                    return true;
                } else if (Failed.equals(job.getLifecycleState())) {
                    return false;
                }
                AppStackDashboard.getAllInstances().forEach(d -> d.populateTableData());

                // Wait a bit before checking again
                Thread.sleep(5000); // Sleep for 5 seconds
            }
        } catch (BmcException e){
            UIUtil.fireNotification(NotificationType.ERROR,e.getMessage(),null);
        }catch (InterruptedException e) {
            // Handle exceptions
            throw new RuntimeException();
        }

        return false;
    };
    public void stopCheckingJobState(){
        isRunning = false;
    }



    public static Job getJob(String jobId) {
        try {
            OracleCloudAccount.ResourceManagerClientProxy resourceManagerClient = OracleCloudAccount.getInstance().getResourceManagerClientProxy();
            return resourceManagerClient.getJobDetails(jobId);
        }catch (BmcException ex){
            Log.error(ex.getMessage());
        }
        return null;

    }


    public  void startBackgroundTask(Project project, String title, String processingMessage, String failedMessage , String succeededMessage ,String jobId, Runnable runLater) {
        Task.Backgroundable task = new Task.Backgroundable(project, title, false) {

            @Override
            public void run(@NotNull ProgressIndicator progressIndicator) {
                // Set the progress indicator's initial text
                progressIndicator.setText(processingMessage+"...");
                progressIndicator.setIndeterminate(true);


                if (progressIndicator.isCanceled()) {
                    return;
                }

                if (isJobFinished.apply(jobId)){
                    progressIndicator.setText(succeededMessage);
                    UIUtil.fireNotification(NotificationType.INFORMATION, succeededMessage, null);
                }else {
                    progressIndicator.setText(failedMessage);
                    UIUtil.fireNotification(NotificationType.ERROR, failedMessage, null);
                }
                // refresh the last job state
                Job job = getJob(jobId);
                AppStackDashboard.getAllInstances().forEach(d -> d.refreshLastJobState(job));
            }

            @Override
            public void onFinished() {
                if (runLater != null){
                    try {
                        runLater.run();
                    } catch (Exception e) {
                        String errorMessage = e.getMessage()==null?"Something went wrong ":e.getMessage();
                        UIUtil.fireNotification(NotificationType.ERROR, errorMessage, null);
                    }
                }

            }
        };

        // Run the task with a progress indicator
        ProgressManager.getInstance().run(task);
    }
    public  void startBackgroundTask(Project project, String title, String processingMessage, String failedMessage , String succeededMessage , String jobId) {
        startBackgroundTask(project,title,processingMessage,failedMessage,succeededMessage,jobId,null);
    }



}