/*
  Copyright (c) 2021, Oracle and/or its affiliates.
  Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */
package com.oracle.oci.intellij.ui.account;

import org.jetbrains.annotations.NotNull;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.util.IconLoader;
import com.oracle.bmc.identity.model.Compartment;
import com.oracle.oci.intellij.account.SystemPreferences;
import com.oracle.oci.intellij.ui.common.CompartmentSelection;

/**
 * Action handler for selection event of UI component 'Compartment'.
 */
public class CompartmentAction extends AnAction {

  public CompartmentAction() {
    super("Compartment", "Select compartment", 
            IconLoader.getIcon("/icons/compartments.png", RegionAction.class));
    //new ImageIcon(
    //RegionAction.class.getResource("/icons/compartments.png")
  }

  /**
   * Event handler.
   *
   * @param event event.
   */
  @Override
  public void actionPerformed(@NotNull AnActionEvent event) {
    final CompartmentSelection compartmentSelection = CompartmentSelection.newInstance();

    if (compartmentSelection.showAndGet()) {
      final Compartment selectedCompartment = compartmentSelection.getSelectedCompartment();
      SystemPreferences.setCompartment(selectedCompartment);
    }
  }

}
