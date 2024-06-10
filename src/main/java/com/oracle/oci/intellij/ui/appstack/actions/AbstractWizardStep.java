package com.oracle.oci.intellij.ui.appstack.actions;

import com.intellij.ui.wizard.WizardModel;
import com.intellij.ui.wizard.WizardNavigationState;
import com.intellij.ui.wizard.WizardStep;
import com.oracle.oci.intellij.ui.appstack.models.Controller;

import javax.swing.*;

public class AbstractWizardStep extends WizardStep {
    boolean dirty = true ;
    Controller controller = Controller.getInstance();
    @Override
    public WizardStep onNext(WizardModel model) {
        CustomWizardModel appStackWizardModel = (CustomWizardModel) model;
        AppStackParametersWizardDialog.isProgramaticChange = true;
        appStackWizardModel.getGroupMenuList().setSelectedIndex(appStackWizardModel.getGroupMenuList().getSelectedIndex()+1);
        AppStackParametersWizardDialog.isProgramaticChange = false;
        return super.onNext(model);
    }





    @Override
    public WizardStep onPrevious(WizardModel model) {

        CustomWizardModel appStackWizardModel = (CustomWizardModel) model;
        AppStackParametersWizardDialog.isProgramaticChange = true;
        appStackWizardModel.getGroupMenuList().setSelectedIndex(appStackWizardModel.getGroupMenuList().getSelectedIndex()-1);
        AppStackParametersWizardDialog.isProgramaticChange = false;
        return super.onPrevious(model);
    }


    @Override
    public JComponent prepare(WizardNavigationState wizardNavigationState) {
        return null;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }
}
