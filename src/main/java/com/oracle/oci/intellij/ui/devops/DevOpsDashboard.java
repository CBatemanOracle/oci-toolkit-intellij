/*
  Copyright (c) 2021, Oracle and/or its affiliates.
  Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */
package com.oracle.oci.intellij.ui.devops;

import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.components.ActionLink;
import com.oracle.bmc.devops.model.ProjectSummary;
import com.oracle.bmc.devops.model.RepositorySummary;
import com.oracle.bmc.resourcemanager.model.StackSummary;
import com.oracle.oci.intellij.account.OracleCloudAccount;
import com.oracle.oci.intellij.account.OracleCloudAccount.DevOpsClientProxy;
import com.oracle.oci.intellij.account.SystemPreferences;
import com.oracle.oci.intellij.ui.appstack.actions.ActionFactory;
import com.oracle.oci.intellij.ui.common.UIUtil;
import com.oracle.oci.intellij.ui.common.UIUtil.ModelHolder;
import com.oracle.oci.intellij.ui.explorer.ITabbedExplorerContent;
import com.oracle.oci.intellij.util.LogHandler;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class DevOpsDashboard implements PropertyChangeListener, ITabbedExplorerContent {

  private JPanel mainPanel;
  // buttons bound in form
  private JButton refreshAppStackButton;
  private JTable devOpsTable;
  private ActionLink profileValueLabel;
  private ActionLink compartmentValueLabel;
  private ActionLink regionValueLabel;
  private JComboBox<ModelHolder<ProjectSummary>> projectCombo;
  private List<ProjectSummary> listDevOpsProjects;
  private Optional<List<RepositorySummary>> listRepositories;
  private @NonNls @NotNull String toolWindowId;
  private @NotNull @NonNls String locationHash;
  private static final CopyOnWriteArrayList<DevOpsDashboard> ALL = new CopyOnWriteArrayList<>();

  public static DevOpsDashboard newInstance(@NotNull Project project,
                                                         @NotNull ToolWindow toolWindow) {
    DevOpsDashboard add = new DevOpsDashboard();
    // they call it a hash but looking at the impl, it looks like
    // name plus full path so should be universally unique?
    // I'd rather not hold a reference to the Project directly because
    // it's clear to me that its an indirect handle like an IProject.
    @NotNull
    @NonNls
    String locationHash = project.getLocationHash();
    add.setProjectLocationHash(locationHash);

    @NonNls
    @NotNull
    String toolId = toolWindow.getId();
    add.setToolWindowId(toolId);

    ALL.add(add);

    return add;
  }

  private void setToolWindowId(@NonNls @NotNull String toolWindowId) {
    this.toolWindowId = toolWindowId;
  }

  public void setProjectLocationHash(@NotNull @NonNls String locationHash) {
    this.locationHash = locationHash;
  }

  public static Stream<DevOpsDashboard> getAllInstances() {
    return ALL.stream();
  }

  public static Optional<DevOpsDashboard> getInstance(@NotNull Project project,
                                                                   @NotNull ToolWindow toolWindow) {
    List<DevOpsDashboard> dashboard =
      getAllInstances().filter(d -> d.toolWindowId.equals(toolWindow.getId()))
                       .collect(Collectors.toList());
    long count = dashboard.size();
    int index = -1;
    if (count > 1) {
      // generally should not happen. but this is too critical, so just use the
      // first one
      index = 0; // TODO:log
    } else if (count == 1) {
      // should always happen unless not initialized on this toolWindow
      index = 0;
    } else {
      // not found.
      return Optional.empty();
    }
    return Optional.of(dashboard.get(index));
  }

  private DevOpsDashboard() {
    initializeProjectCombo();
    initializeTableStructure();

    if (refreshAppStackButton != null) {
      refreshAppStackButton.setAction(new RefreshAction(this, "Refresh"));
    }

    if (profileValueLabel != null){
      profileValueLabel.setAction(ActionFactory.getProfileAction());
    }

    if (compartmentValueLabel != null){
      compartmentValueLabel.setAction(ActionFactory.getCompartmentAction());
    }

    if (regionValueLabel != null){
      regionValueLabel.setAction(ActionFactory.getRegionAction());
    }
    
//    if (createAppStackButton != null) {
//      createAppStackButton.setAction(new CreateAction(this, "Create New AppStack"));
//    }
//    
//    if (deleteAppStackButton != null) {
//      deleteAppStackButton.setAction(new DeleteAction(this, "Delete AppStack"));
//    }
    initializeLabels();
    SystemPreferences.addPropertyChangeListener(this);
  }

  private void initializeProjectCombo() {
    if (projectCombo == null) {
      LogHandler.info("Skipping WorkLoadTypeFilter; form not populated");
      return;
    }
   
    projectCombo.addItemListener(new ItemListener() {
      @SuppressWarnings("unchecked")
      @Override
      public void itemStateChanged(ItemEvent e) {
        final Runnable fetchData = () -> {
          try {
            ModelHolder<ProjectSummary> item = (ModelHolder<ProjectSummary>) e.getItem();
            ProjectSummary projectSummary = item.get();
            listRepositories = Optional.ofNullable(
              OracleCloudAccount.getInstance().getDevOpsClient().listRepositories(projectSummary));

          } catch (Exception exception) {
            listRepositories = Optional.empty();
            UIUtil.fireNotification(NotificationType.ERROR, "Error listing repos"+exception.getMessage());
//            LogHandler.error(exception.getMessage(), exception);
          }
        };
        
        final Runnable updateUI = () -> {
          final DefaultTableModel model = ((DefaultTableModel) devOpsTable.getModel());
          model.setRowCount(0);
           final Object[] rowData = new Object[DevOpsTableModel.DEVOPS_COLUMN_NAMES.length];
          listRepositories.ifPresent(summaries->summaries.forEach(summary -> {
            rowData[0] = summary.getName();
            rowData[1] = summary.getDescription();
            rowData[2] = summary.getLifecycleState();
            rowData[3] = summary.getTimeCreated();
            model.addRow(rowData);
          }));

          refreshAppStackButton.setEnabled(true);
        };
        
        UIUtil.executeAndUpdateUIAsync(fetchData, updateUI);
    }});
  }
  
  private void initializeLabels() {
    if (profileValueLabel == null || compartmentValueLabel == null || regionValueLabel == null) {
      LogHandler.info("Skipping Labels; form not populated");
      return;
    }
    profileValueLabel.setText(SystemPreferences.getProfileName());
    compartmentValueLabel.setText(SystemPreferences.getCompartmentName());
    regionValueLabel.setText(SystemPreferences.getRegionName());
  }

  private void initializeTableStructure() {
    if (devOpsTable != null) {
      devOpsTable.setModel(new DevOpsTableModel(0));
    }
   
  }

  private static class LoadStackJobsAction extends AbstractAction {

    private static final long serialVersionUID = 1463690182909882687L;
    //private StackSummary stack;
    
    public LoadStackJobsAction(StackSummary stack) {
      super("View Jobs..");
      //this.stack = stack;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
//      ResourceManagerClientProxy resourceManagerClientProxy = OracleCloudAccount.getInstance().getResourceManagerClientProxy();
//      GetStackJobsCommand command = new GetStackJobsCommand(resourceManagerClientProxy, 
//                                        stack.getCompartmentId(), stack.getId());
//      // TODO: bother with command stack?
//      try {
//        GetStackJobsResult execute = command.execute();
//        if (execute.isOk()) {
//          
//          execute.getJobs().forEach(job -> System.out.println(job));
//          StackJobDialog dialog = new StackJobDialog(execute.getJobs());
//          dialog.show();
//
//        }
//        else if (execute.getException() != null) {
//          throw execute.getException();
//        }
//      } catch (Throwable e1) {
//        throw new RuntimeException(e1);
//      }
    }
  }
  private static class LoadAppUrlAction extends AbstractAction {

    private static final long serialVersionUID = -5434743269928138930L;

    public LoadAppUrlAction(StackSummary stack) {
      super("Launch App Url..");
     // this.stack = stack;
    }

    @Override
    public void actionPerformed(ActionEvent event) {
      Desktop desktop = java.awt.Desktop.getDesktop();
      try {
        //specify the protocol along with the URL
        URI oURL = new URI(
            "http://129.153.104.43");
        desktop.browse(oURL);
      } catch (URISyntaxException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace(); 
      }
    }
    
  }

  @SuppressWarnings("unused")
  private JPopupMenu getStackSummaryActionMenu(StackSummary selectedSummary) {
    final JPopupMenu popupMenu = new JPopupMenu();
//
//    popupMenu.add(new JMenuItem(new RefreshAction(this, "Refresh")));
//    popupMenu.addSeparator();
//
    if (selectedSummary != null) {
      popupMenu.add(new JMenuItem(new LoadStackJobsAction(selectedSummary)));
      popupMenu.add(new JMenuItem(new LoadAppUrlAction(selectedSummary)));
     
//      if (selectedSummary.getLifecycleState() == LifecycleState.Available) {

//        popupMenu.add(new JMenuItem(new AutonomousDatabaseMoreActions(
//                AutonomousDatabaseMoreActions.Action.RESTORE_ADB,
//                selectedSummary, "Restore")));
//
//        popupMenu.add(new JMenuItem(new AutonomousDatabaseMoreActions(
//                AutonomousDatabaseMoreActions.Action.CLONE_DB,
//                selectedSummary, "Create Clone")));
//
//        popupMenu.add(new JMenuItem(new AutonomousDatabaseMoreActions(
//                AutonomousDatabaseMoreActions.Action.ADMIN_PWD_CHANGE,
//                selectedSummary, "Administrator Password")));
//
//        popupMenu.add(new JMenuItem(
//                new AutonomousDatabaseMoreActions(AutonomousDatabaseMoreActions.Action.UPDATE_LICENSE,
//                        selectedSummary, "Update License Type")));
//
//        popupMenu.add(new JMenuItem(
//                new AutonomousDatabaseMoreActions(AutonomousDatabaseMoreActions.Action.DOWNLOAD_CREDENTIALS,
//                        selectedSummary, "Download Client Credentials (Wallet)")));
//
//        popupMenu.add(new JMenuItem(
//                new AutonomousDatabaseMoreActions(AutonomousDatabaseMoreActions.Action.UPDATE_NETWORK_ACCESS,
//                        selectedSummary, "Update Network Access")));
//
//        popupMenu.add(new JMenuItem(new AutonomousDatabaseMoreActions(AutonomousDatabaseMoreActions.Action.SCALE_ADB,
//                selectedSummary, "Scale Up/Down")));
//
//        if (selectedSummary.getDbWorkload().equals(
//                AutonomousDatabaseSummary.DbWorkload.Ajd)) {
//          popupMenu.add(new JMenuItem(new AutonomousDatabaseBasicActions(
//                  selectedSummary, AutonomousDatabaseBasicActions.ActionType.CHANGE_WORKLOAD_TYPE)));
//        }
//
//        popupMenu.add(new JMenuItem(new AutonomousDatabaseBasicActions(selectedSummary,
//                AutonomousDatabaseBasicActions.ActionType.TERMINATE)));
//
//        popupMenu.add(new JMenuItem(new AutonomousDatabaseMoreActions(
//                AutonomousDatabaseMoreActions.Action.ADB_INFO,
//                selectedSummary,
//                "Autonomous Database Information")));
//
//        popupMenu.addSeparator();
//      } else if (selectedSummary.getLifecycleState() == LifecycleState.Stopped) {
//        popupMenu.add(new JMenuItem(new AutonomousDatabaseBasicActions(selectedSummary,
//                AutonomousDatabaseBasicActions.ActionType.START)));
//
//        popupMenu.add(new JMenuItem(new AutonomousDatabaseMoreActions(
//                AutonomousDatabaseMoreActions.Action.CLONE_DB,
//                selectedSummary, "Create Clone")));
//
//        popupMenu.add(new JMenuItem(new AutonomousDatabaseBasicActions(selectedSummary,
//                AutonomousDatabaseBasicActions.ActionType.TERMINATE)));
//      } else if (selectedSummary.getLifecycleState() == LifecycleState.Provisioning) {
//        popupMenu.add(new JMenuItem(new AutonomousDatabaseBasicActions(selectedSummary,
//                AutonomousDatabaseBasicActions.ActionType.TERMINATE)));
//      }
//
//      popupMenu.add(new JMenuItem(new AutonomousDatabaseMoreActions(AutonomousDatabaseMoreActions.Action.CREATE_ADB,
//              selectedSummary, CREATE_ADB)));
//
//      popupMenu.add(new JMenuItem(new AutonomousDatabaseBasicActions(
//              selectedSummary, AutonomousDatabaseBasicActions.ActionType.SERVICE_CONSOLE)));
    }
    return popupMenu;
  }

  public void populateTableData() {
//    ((DefaultTableModel) appStacksTable.getModel()).setRowCount(0);
   UIUtil.showInfoInStatusBar("Refreshing DevOps Projects.");

    refreshAppStackButton.setEnabled(false);

    final Runnable fetchData = () -> {
      try {
        DevOpsClientProxy devOpsClient = OracleCloudAccount.getInstance().getDevOpsClient();
        this.listDevOpsProjects = devOpsClient.listDevOpsProjects();
      } catch (Exception exception) {
        listDevOpsProjects = null;
        UIUtil.fireNotification(NotificationType.ERROR, "Problem with refresh"+exception.getMessage());
        LogHandler.error(exception.getMessage(), exception);
      }
    };

    final Runnable updateUI = () -> {
      if (this.listDevOpsProjects != null) {
        UIUtil.showInfoInStatusBar((listDevOpsProjects.size()) + " DevOps Projects found.");
        projectCombo.removeAllItems();

        for (ProjectSummary s : this.listDevOpsProjects) {
          projectCombo.addItem(UIUtil.holdModel(s).setTextProvider(summary -> {return summary.getName();}));
        }
      }
      refreshAppStackButton.setEnabled(true);
    };
//    
    UIUtil.executeAndUpdateUIAsync(fetchData, updateUI);
  }

//  private ImageIcon getStatusImage(LifecycleState state) {
//    if (state.equals(LifecycleState.Available) || state
//            .equals(LifecycleState.ScaleInProgress)) {
//      return new ImageIcon(getClass().getResource("/icons/db-available-state.png"));
//    } else if (state.equals(LifecycleState.Terminated) || state
//            .equals(LifecycleState.Unavailable)) {
//      return new ImageIcon(getClass().getResource("/icons/db-unavailable-state.png"));
//    } else {
//      return new ImageIcon(getClass().getResource("/icons/db-inprogress-state.png"));
//    }
//  }

  public JComponent createCenterPanel() {
    return mainPanel;
  }

  @Override
  public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
    LogHandler.info("AutonomousDatabasesDashboard: Handling the Event Update : " + propertyChangeEvent.toString());
    ((DefaultTableModel) devOpsTable.getModel()).setRowCount(0);

    switch (propertyChangeEvent.getPropertyName()) {
      case SystemPreferences.EVENT_COMPARTMENT_UPDATE:
        compartmentValueLabel.setText(SystemPreferences.getCompartmentName());
        break;

      case SystemPreferences.EVENT_REGION_UPDATE:
        regionValueLabel.setText(propertyChangeEvent.getNewValue().toString());
        break;

      case SystemPreferences.EVENT_SETTINGS_UPDATE:
      case SystemPreferences.EVENT_ADB_INSTANCE_UPDATE:
        profileValueLabel.setText(SystemPreferences.getProfileName());
        compartmentValueLabel.setText(SystemPreferences.getCompartmentName());
        regionValueLabel.setText(SystemPreferences.getRegionName());
        break;
    }
    UIUtil.schedule(()->{
      populateTableData();
    });
  }

  private static class RefreshAction extends AbstractAction {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private final DevOpsDashboard devOpsDashBoard;

    public RefreshAction(DevOpsDashboard adbDetails, String actionName) {
      super(actionName);
      this.devOpsDashBoard = adbDetails;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      devOpsDashBoard.populateTableData();
    }
  }

//  private static class CreateAction extends AbstractAction {
//    /**
//     *
//     */
//    private static final long serialVersionUID = 1L;
//    private DevOpsDashboard dashboard;
//
//    public CreateAction(DevOpsDashboard dashboard, String actionName) {
//      super(actionName);
//      this.dashboard = dashboard;
//    }
//
//    @Override
//    public void actionPerformed(ActionEvent e) {

//      AtomicReference<Map<String, String>> variables = new AtomicReference<>(new LinkedHashMap<>());
//
//      dashboard.createAppStackButton.setEnabled(false);
//      Runnable runnable = () -> {
//        YamlLoader loader = new YamlLoader();
//        dashboard.createAppStackButton.setEnabled(true);
//
//        try {
//          variables.set(loader.load());
//        } catch (IntrospectionException | IllegalAccessException | InvocationTargetException ex) {
//          throw new RuntimeException(ex);
//        }
//        if (variables.get() == null)
//          return;
//        try {
//          ResourceManagerClientProxy proxy = OracleCloudAccount.getInstance().getResourceManagerClientProxy();
//          //todo get the compartment from the form , that i will add in the stack infos
//          String compartmentId = SystemPreferences.getCompartmentId();
//          ClassLoader cl = DevOpsDashboard.class.getClassLoader();
//          CreateStackCommand command =
//                  new CreateStackCommand(proxy, compartmentId, cl, "appstackforjava.zip");
////          Map<String,String> variables = new ModelLoader().loadTestVariables();
////          variables.put("shape","CI.Standard.E3.Flex");
//          command.setVariables(variables.get());
////          command.setVariables(variables.get());
//          this.dashboard.commandStack.execute(command);
//        } catch (Exception e1) {
//          throw new RuntimeException(e1);
//        }
//      };
//      ApplicationManager.getApplication().invokeLater(runnable);

//    }
//  }
  
//  public static class DeleteAction extends AbstractAction {
//
//    private static final long serialVersionUID = 7216149349340773007L;
//    private DevOpsDashboard dashboard;
//    public DeleteAction(DevOpsDashboard dashboard, String title) {
//      super("Delete");
//      this.dashboard = dashboard;
//    }
//    
//    private static class DeleteYesNoDialog extends DialogWrapper {
//
//      protected DeleteYesNoDialog() {
//        super(true);
//        init();
//        setTitle("Confirm Delete");
//        setOKButtonText("Ok");
//      }
//
//      @Override
//      protected @Nullable JComponent createNorthPanel() {
//        JPanel northPanel = new JPanel();
//        JLabel label = new JLabel();
//        label.setText("Delete Stack.  Are you sure?");
//        northPanel.add(label);
//        return northPanel;
//      }
//
//      @Override
//      protected @NotNull JPanel createButtonsPanel(@NotNull List<? extends JButton> buttons) {
//        return new JPanel();
//      }
//
//      @Override
//      protected @Nullable JComponent createCenterPanel() {
//        JPanel messagePanel = new JPanel(new BorderLayout());
//
//        JPanel yesNoButtonPanel = new JPanel();
//        yesNoButtonPanel.setLayout(new BoxLayout(yesNoButtonPanel, BoxLayout.X_AXIS));
//        JButton yesButton = new JButton();
//        yesButton.setText("Yes");
//        yesButton.addActionListener(new ActionListener() { 
//          @Override
//          public void actionPerformed(ActionEvent e) {
//            close(OK_EXIT_CODE);
//          }
//        });
//        yesNoButtonPanel.add(yesButton);
//        JButton noButton = new JButton();
//        noButton.setText("No");
//        noButton.addActionListener(new ActionListener() {
//
//          @Override
//          public void actionPerformed(ActionEvent e) {
//            close(CANCEL_EXIT_CODE);
//          }
//          
//        });
//        yesNoButtonPanel.add(noButton);
//        
//        messagePanel.add(yesNoButtonPanel, BorderLayout.CENTER);
//        
//        JCheckBox myCheckBox = new JCheckBox();
//        myCheckBox.setText("Destroy stack before deleting");
//        myCheckBox.setSelected(true);
//        messagePanel.add(myCheckBox, BorderLayout.SOUTH);
//
//       // pack();
//
//        return messagePanel;
//      }
//      
//    }
//    @Override
//    public void actionPerformed(ActionEvent e) {
//     
//    }
//  }

  @Override
  public String getTitle() {
    return "DevOps";
  }


}