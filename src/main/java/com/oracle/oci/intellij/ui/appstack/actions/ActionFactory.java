package com.oracle.oci.intellij.ui.appstack.actions;

import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.oracle.bmc.identity.model.Compartment;
import com.oracle.oci.intellij.account.SystemPreferences;
import com.oracle.oci.intellij.ui.account.ConfigureOracleCloudDialog;
import com.oracle.oci.intellij.ui.account.RegionAction;
import com.oracle.oci.intellij.ui.common.CompartmentSelection;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;

public class ActionFactory {

    public static AbstractAction getProfileAction(){
       return new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ConfigureOracleCloudDialog.newInstance().showAndGet();
            }
        };
    }

    public static AbstractAction getCompartmentAction(){
        return new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final CompartmentSelection compartmentSelection = CompartmentSelection.newInstance();

                if (compartmentSelection.showAndGet()) {
                    final Compartment selectedCompartment = compartmentSelection.getSelectedCompartment();
                    SystemPreferences.setCompartment(selectedCompartment);
                }
            }
        };
    }

    public static AbstractAction getRegionAction(){
        return new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                RegionAction regionAction = new RegionAction();
                Object source = e.getSource();
                DataContext dataContext = ActionToolbar.getDataContextFor((Component) source);

                MouseEvent simulatedMouseEvent = new MouseEvent((Component) source, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(),
                        0, 0, 0, 1, false);

                AnActionEvent anActionEvent = AnActionEvent.createFromAnAction(regionAction, simulatedMouseEvent,
                        ActionPlaces.UNKNOWN, dataContext);

                regionAction.actionPerformed(anActionEvent);
            }
        };
    }
}
