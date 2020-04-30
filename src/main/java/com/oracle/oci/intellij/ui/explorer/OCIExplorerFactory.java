package com.oracle.oci.intellij.ui.explorer;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.openapi.wm.ex.ToolWindowEx;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;

import com.oracle.oci.intellij.account.AuthProvider;
import com.oracle.oci.intellij.account.IdentClient;
import com.oracle.oci.intellij.account.PreferencesWrapper;
import com.oracle.oci.intellij.ui.account.CompartmentSelectionAction;
import com.oracle.oci.intellij.ui.account.OCISettingsAction;
import com.oracle.oci.intellij.ui.account.RegionSelectionAction;
import com.oracle.oci.intellij.ui.common.UIUtil;
import com.oracle.oci.intellij.ui.database.ADBInstanceClient;
import com.oracle.oci.intellij.ui.database.DatabaseDetails;
import org.jetbrains.annotations.NotNull;



public class OCIExplorerFactory
    implements ToolWindowFactory {

  private final DatabaseDetails databaseDetails;

  public OCIExplorerFactory() {

    // Property Event Change has to be updated in this order.
    PreferencesWrapper.addPropertyChangeListener(AuthProvider.getInstance());
    PreferencesWrapper.addPropertyChangeListener(IdentClient.getInstance());
    PreferencesWrapper.addPropertyChangeListener(ADBInstanceClient.getInstance());

    databaseDetails = new DatabaseDetails();
    PreferencesWrapper.addPropertyChangeListener(databaseDetails);
  }

  @Override
  public void createToolWindowContent(@NotNull Project project,
      @NotNull ToolWindow toolWindow) {
    UIUtil.setCurrentProject(project);
    final ToolWindowEx toolWindowEx = (ToolWindowEx) toolWindow;

    // This actions are available on the top right of the toolbar
    final DefaultActionGroup actionGroup = new DefaultActionGroup();
    actionGroup.add(new OCISettingsAction());
    actionGroup.add(new RegionSelectionAction());
    actionGroup.add(new CompartmentSelectionAction());
    toolWindowEx.setTitleActions(actionGroup);

    final TabbedExplorer ociTabbedToolBar = new TabbedExplorer(toolWindow, databaseDetails);
    final ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
    final Content ociTabbedToolBarContent = contentFactory
        .createContent(ociTabbedToolBar.getContent(), "", false);
    toolWindow.getContentManager().addContent(ociTabbedToolBarContent);

  }

}
