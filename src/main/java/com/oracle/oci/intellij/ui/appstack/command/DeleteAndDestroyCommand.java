package com.oracle.oci.intellij.ui.appstack.command;

import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.ProjectManager;
import com.oracle.bmc.resourcemanager.responses.CreateJobResponse;
import com.oracle.oci.intellij.account.OracleCloudAccount;
import com.oracle.oci.intellij.ui.common.MyBackgroundTask;
import com.oracle.oci.intellij.ui.common.UIUtil;


public class DeleteAndDestroyCommand extends DestroyStackCommand {

    public DeleteAndDestroyCommand(OracleCloudAccount.ResourceManagerClientProxy resourceManagerClientProxy, String stackId, String stackName) {
        super(resourceManagerClientProxy, stackId, stackName);
    }

    @Override
    protected Result doExecute() throws Exception {
        CreateJobResponse response = this.resManagerClientProxy.destroyStack(stackId);
        String applyJobId = response.getJob().getId();

        DeleteStackCommand deleteCommand = new DeleteStackCommand(this.resManagerClientProxy, this.stackId,stackName);

        Runnable deleteAfterDestroy = ()->{
            try {
                deleteCommand.execute();
            } catch (Exception e) {
                String errorMessage = e.getMessage()==null?"Something went wrong ":e.getMessage();
                UIUtil.fireNotification(NotificationType.ERROR, "Error in delete:"+errorMessage);
            }
        };

        new MyBackgroundTask().startBackgroundTask(ProjectManager.getInstance().getDefaultProject(),"Destroying Resources of \""+stackName+"\" (stack)","Destroying resources...","Destroy Job Failed please check logs of \""+stackName+"\" (stack)","Destroy job successfully applied on \""+stackName+"\" (stack)",applyJobId,deleteAfterDestroy);
        UIUtil.fireNotification(NotificationType.INFORMATION, "Destroy Job was submitted for \""+stackName+"\" (stack)");
        return new Result(Result.Severity.NONE, Result.Status.OK);
    }
}

