package com.oracle.oci.intellij.ui.appstack.models;


import com.intellij.openapi.project.ProjectManager;
import com.oracle.oci.intellij.settings.OCIApplicationSettings;
import com.oracle.oci.intellij.ui.appstack.actions.PropertyOrder;
import com.oracle.oci.intellij.ui.appstack.annotations.VariableMetaData;

public class IntroductoryStep extends VariableGroup {
    private String  descriptionText ;
    private boolean introductionDontShowAgain;

    @PropertyOrder(1)
    @VariableMetaData(type = "textArea")
    public String getDescriptionText() {
        return descriptionText;
    }

    public void setDescriptionText(String descriptionText) {
        this.descriptionText = descriptionText;
    }
    @VariableMetaData(type = "boolean",title = "do not show again ?",required = true)
    public boolean isIntroductionDontShowAgain() {
        return introductionDontShowAgain;
    }

    public void setIntroductionDontShowAgain(boolean introductionDontShowAgain) {
        this.introductionDontShowAgain = introductionDontShowAgain;

        OCIApplicationSettings.State state =  OCIApplicationSettings.getInstance(ProjectManager.getInstance().getDefaultProject()).getState();
        state.setAppStackIntroductoryStepShow(!introductionDontShowAgain);

    }
}