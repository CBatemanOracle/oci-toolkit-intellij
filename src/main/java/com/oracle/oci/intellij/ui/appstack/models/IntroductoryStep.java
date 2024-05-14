package com.oracle.oci.intellij.ui.appstack.models;


import com.oracle.oci.intellij.ui.appstack.actions.PropertyOrder;
import com.oracle.oci.intellij.ui.appstack.annotations.VariableMetaData;

public class IntroductoryStep extends VariableGroup {
    private String  descriptionText ;
    private boolean introductionDontShowAgain;

    @PropertyOrder(1)
    @VariableMetaData(title = "", type = "textArea")
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

    }
}