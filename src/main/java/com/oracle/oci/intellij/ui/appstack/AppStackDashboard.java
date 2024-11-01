/*
  Copyright (c) 2021, Oracle and/or its affiliates.
  Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */
package com.oracle.oci.intellij.ui.appstack;

import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.components.ActionLink;
import com.intellij.ui.table.JBTable;
import com.oracle.bmc.model.BmcException;
import com.oracle.bmc.resourcemanager.model.Stack;
import com.oracle.bmc.resourcemanager.model.*;
import com.oracle.bmc.resourcemanager.responses.CreateJobResponse;
import com.oracle.oci.intellij.account.OracleCloudAccount;
import com.oracle.oci.intellij.account.OracleCloudAccount.ResourceManagerClientProxy;
import com.oracle.oci.intellij.account.SystemPreferences;
import com.oracle.oci.intellij.common.command.AbstractBasicCommand;
import com.oracle.oci.intellij.common.command.AbstractBasicCommand.CommandFailedException;
import com.oracle.oci.intellij.common.command.AbstractBasicCommand.Result;
import com.oracle.oci.intellij.common.command.CommandStack;
import com.oracle.oci.intellij.common.command.CompositeCommand;
import com.oracle.oci.intellij.ui.appstack.actions.ActionFactory;
import com.oracle.oci.intellij.ui.appstack.actions.ReviewDialog;
import com.oracle.oci.intellij.ui.appstack.command.*;
import com.oracle.oci.intellij.ui.appstack.command.GetStackJobsCommand.GetStackJobsResult;
import com.oracle.oci.intellij.ui.appstack.exceptions.JobRunningException;
import com.oracle.oci.intellij.ui.appstack.models.Utils;
import com.oracle.oci.intellij.ui.appstack.models.proxies.StackProxy;
import com.oracle.oci.intellij.ui.appstack.uimodel.AppStackTableModel;
import com.oracle.oci.intellij.ui.common.Icons;
import com.oracle.oci.intellij.ui.common.InformationDialog;
import com.oracle.oci.intellij.ui.common.UIUtil;
import com.oracle.oci.intellij.ui.common.cache.SimpleCache;
import com.oracle.oci.intellij.ui.explorer.ITabbedExplorerContent;
import com.oracle.oci.intellij.util.LogHandler;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.IntrospectionException;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class AppStackDashboard implements PropertyChangeListener, ITabbedExplorerContent {

  private JPanel mainPanel;
  //private JComboBox<String> workloadCombo;
  // buttons bound in form
  private JButton refreshAppStackButton;
  private JButton deleteAppStackButton;
  private JButton createAppStackButton;
  private JButton applyAppStackButton;
  private JBTable appStacksTable;
  private ActionLink profileValueLabel;
  private ActionLink compartmentValueLabel;
  private ActionLink regionValueLabel;
  private JComboBox<String> filerStackCombo;
  private JButton infoButton;
  static public String APP_STACK = "App stack";
  String ALL_STACK = "All";
  private List<StackProxy> appStackList;
  private CommandStack commandStack = new CommandStack();
  private @NonNls @NotNull String toolWindowId;
  private @NotNull @NonNls String locationHash;
  private static ResourceBundle resBundle;
  public static SimpleCache jobsStackCache;
  private static final CopyOnWriteArrayList<AppStackDashboard> ALL = new CopyOnWriteArrayList<>();

  public static AppStackDashboard newInstance(@NotNull Project project,
                                                         @NotNull ToolWindow toolWindow) {
    AppStackDashboard add = new AppStackDashboard();
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

  /**
   * @return all the instances of the dashboard; in most cases, we must stream
   * updates onto all of them.
   */
  public static Stream<AppStackDashboard> getAllInstances() {
    return ALL.stream();
  }

  public static Optional<AppStackDashboard> getInstance(@NotNull Project project,
                                                      @NotNull ToolWindow toolWindow) {
    List<AppStackDashboard> dashboard =
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

  private AppStackDashboard() {
    jobsStackCache = new SimpleCache<String,List<JobSummary>>();
    // initiate property descriptors .... so we can build the form for the show appStack details .....
    YamlLoader load = new YamlLoader();
    try {
      Utils.descriptorsState = load.load1(Utils.variableGroups);
    } catch (IntrospectionException e) {
      throw new RuntimeException(e);
    }
    //initializeWorkLoadTypeFilter();
    initializeTableStructure();

    if (refreshAppStackButton != null) {
      refreshAppStackButton.setAction(new RefreshAction(this, "Refresh"));
    }
    
    if (createAppStackButton != null) {
      createAppStackButton.setAction(new CreateAction(this, "Create New AppStack"));
    }
    
    if (deleteAppStackButton != null) {
      deleteAppStackButton.setAction(new DeleteAction(this, "Delete AppStack"));
    }
    if (applyAppStackButton != null) {
      applyAppStackButton.setAction(new ApplyAction(this, "Apply AppStack"));
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

    initializeLabels();
    resBundle = ResourceBundle.getBundle("appStackDashboard", Locale.ROOT);
    SystemPreferences.addPropertyChangeListener(this);

    UIUtil.invokeLater(this::populateTableData);
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
    if (appStacksTable == null) {
      return;
    }
    infoButton.addActionListener(e->{
      InformationDialog informationDialog = new InformationDialog(resBundle.getString("DROPDOWN_FILTER_TOOLTIP_TEXT"));
      informationDialog.show();
    });
    appStacksTable.setModel(new AppStackTableModel(0));

    appStacksTable.getColumn("Last Job State").setCellRenderer(new DefaultTableCellRenderer() {
      @Override
      public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        if (column == 5) {
          super.getTableCellRendererComponent(table,value,isSelected,hasFocus,row,column);
          final JobSummary job = (JobSummary) value;
          if (job == null ) return null;

          StringBuilder columnText = new StringBuilder();
          columnText.append(job.getOperation());
          columnText.append(" Job  ->  ");
          columnText.append(job.getLifecycleState());

          this.setText(columnText.toString());
          this.setIcon(getImageStatus(job.getLifecycleState()));
          return this;
        }
        return (Component) value;
      }
    });


    appStacksTable.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e){
        if (e.getButton() == 3) {
          JPopupMenu popupMenu;
          StackProxy selectedSummary = null;
          if (appStacksTable.getSelectedRowCount() == 1) {
            StackProxy selectedObject = appStackList.get(appStacksTable.getSelectedRow());
            if (selectedObject != null) {
              selectedSummary = selectedObject;
            }
          }
          // TODO:
          System.out.println("Pop!");
          if (selectedSummary != null){
            popupMenu = getStackSummaryActionMenu(selectedSummary.getStackSummary());
            popupMenu.show(e.getComponent(), e.getX(), e.getY());
          }
        }
      }
    });

    filerStackCombo.addItemListener(e->{
      filterStacks();
    });
    filerStackCombo.setSelectedItem(APP_STACK);

  }

  private void filterStacks() {
    if (appStackList != null){
      List <StackProxy> filtredStackList;
      if (APP_STACK.equals(filerStackCombo.getSelectedItem())){

        filtredStackList = appStackList.stream().filter(item-> APP_STACK.equals(item.getStackSummary().getFreeformTags().get("Type"))).collect(Collectors.toList());
      }else {
        filtredStackList = appStackList;
      }
      populateTable(filtredStackList);
    }
  }

  private JobSummary getLastJob(String stackId, String compartmentId) {
    JobSummary lastAppliedJob = null;
    try {
      lastAppliedJob= (JobSummary) OracleCloudAccount.getInstance().getResourceManagerClientProxy().getLastJob(stackId,compartmentId);

    }catch (RuntimeException exception){
      UIUtil.fireNotification(NotificationType.ERROR,"Something Went Wrong ");
    }
    return lastAppliedJob;
  }
  public static Icon getImageStatus(Job.LifecycleState state){

    switch (state) {
      case Accepted:
        return IconLoader.getIcon(Icons.ACCEPTED.getPath());
      case Canceled:
        return IconLoader.getIcon(Icons.CANCELED.getPath());
      case Canceling:
        return IconLoader.getIcon(Icons.CANCELING.getPath());
      case Failed:
        return IconLoader.getIcon(Icons.FAILED.getPath());
      case InProgress:
        return IconLoader.getIcon(Icons.IN_PROGRESS.getPath());
      case Succeeded:
        return IconLoader.getIcon(Icons.SUCCEEDED.getPath());
      default:
        return null; // Or a default icon
    }
  }

  public synchronized void refreshLastJobState( Job job) {
    String stackId = job.getStackId();
    int stackRow = getStackRow(stackId);
    if (stackRow != -1){
      DefaultTableModel model = (DefaultTableModel) appStacksTable.getModel();
      final JobSummary[] jobSummary = {null};
      Runnable fetchData = ()->{
        jobSummary[0] =  OracleCloudAccount.getInstance().getResourceManagerClientProxy().listJobs(job.getCompartmentId(), job.getStackId()).getItems().get(0);
      };
      Runnable updateUI = ()->{
        System.out.println(model.getRowCount()+"  "+stackRow);
//        if (model.getRowCount() <= stackRow){
          try {
            model.setValueAt(jobSummary[0], stackRow, 5);
            // Notify the table that this particular cell has been updated
            model.fireTableCellUpdated(stackRow, 5);
          }catch (ArrayIndexOutOfBoundsException ex){
            System.out.println(ex.getMessage());
          }

//        }

      };

      UIUtil.executeAndUpdateUIAsync(fetchData,updateUI);

    }

  }


  private int getStackRow(String stackId) {
    for (int i = 0; i < appStackList.size(); i++) {
      StackProxy object = appStackList.get(i);
      if (object.getStackSummary().getId().equals(stackId)) {
        return i;
      }
    }
    return -1;
  }

  private void createUIComponents() {
    // TODO: place custom component creation code here
    infoButton = new UIUtil.IconButton(Icons.INFO.getPath());

  }

  private static class LoadStackJobsAction extends AbstractAction {

    private static final long serialVersionUID = 1463690182909882687L;
    private StackSummary stack;
    
    public LoadStackJobsAction(StackSummary stack) {
      super("View Jobs..");
      this.stack = stack;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      final List<JobSummary>[] jobSummaries = new List[]{null};
      Runnable fetchData = ()->{
        ResourceManagerClientProxy resourceManagerClientProxy = OracleCloudAccount.getInstance().getResourceManagerClientProxy();
        GetStackJobsCommand command = new GetStackJobsCommand(resourceManagerClientProxy,
                stack.getCompartmentId(), stack.getId());
        // TODO: bother with command stack?
        try {
          GetStackJobsResult execute = command.execute();
          if (execute.isOk()) {

            execute.getJobs().forEach(job -> System.out.println(job));
            jobSummaries[0] = execute.getJobs();

          }
          else if (execute.getException() != null) {
            throw execute.getException();
          }
        } catch (Throwable e1) {
          UIUtil.fireNotification(NotificationType.ERROR,"Something Went Wrong ");
//        throw new RuntimeException(e1);
        }
      };

      Runnable updateUI = ()->{
        StackJobDialog dialog = new StackJobDialog(jobSummaries[0]);
        dialog.show();
      };

      UIUtil.executeAndUpdateUIAsync(fetchData,updateUI);

    }
  }

  private static class ShowStackDetailsAction extends AbstractAction {

    private static final long serialVersionUID = 1463690182909882687L;
    private final StackSummary stack;

    public ShowStackDetailsAction(StackSummary stack) {
      super("Show Stack Details..");
      this.stack = stack;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      AtomicReference<Map<String, String>> variables = new AtomicReference<>();
      Runnable fetchData = ()->{
        ResourceManagerClientProxy resourceManagerClientProxy = OracleCloudAccount.getInstance().getResourceManagerClientProxy();
        Stack stackDetails =  resourceManagerClientProxy.getStackDetails(stack.getId());
       variables.set(stackDetails.getVariables());
      };

      Runnable updateUI = ()->{
        ReviewDialog reviewDialog = new ReviewDialog(variables.get(), Utils.variableGroups,true);

        reviewDialog.showAndGet();
        reviewDialog.close(200);
      };

      UIUtil.executeAndUpdateUIAsync(fetchData,updateUI);

    }
  }
  private static class LoadAppUrlAction extends AbstractAction {
    StackSummary stack ;
    public LoadAppUrlAction(StackSummary stack) {
      super("Launch App Url..");
      this.stack = stack;
    }

    @Override
    public void actionPerformed(ActionEvent event) {
      Desktop desktop = Desktop.getDesktop();
      final String[] applicationUrl = {null};

      Runnable fetchData =()->{
        JobSummary lastApplyJob = getLastApplyJob();
        try {
          applicationUrl[0] =  getUrlOutput(lastApplyJob.getId());
        } catch (Exception e) {
          UIUtil.fireNotification(NotificationType.ERROR,"Something Went Wrong ");
          LogHandler.error(e.getMessage(), e);

//            throw new RuntimeException(e);
        }
      };

      Runnable updateUI = ()->{
        if (applicationUrl[0].isEmpty()){
          UIUtil.fireNotification(NotificationType.ERROR,"Problem retrieving the application url , you can always  try to get url from apply job logs");
          return;
        }
        try {
          //specify the protocol along with the URL
          URI oURL = new URI(
                  applicationUrl[0]);
          desktop.browse(oURL);
        } catch (URISyntaxException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        } catch (IOException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      };

        UIUtil.executeAndUpdateUIAsync(fetchData,updateUI);

    }


    private JobSummary getLastApplyJob() {
      ResourceManagerClientProxy resourceManagerClientProxy = OracleCloudAccount.getInstance().getResourceManagerClientProxy();
      GetStackJobsCommand command = new GetStackJobsCommand(resourceManagerClientProxy,
              stack.getCompartmentId(), stack.getId());
      try {
        GetStackJobsResult execute = command.execute();
        if (execute.isOk()) {

          execute.getJobs().forEach(job -> System.out.println(job));
          Optional<JobSummary> lastApplyJob = execute.getJobs().stream().filter((job)->job.getOperation().equals(Job.Operation.Apply)).findFirst();
          //todo check if that job is applied successfully
          return  lastApplyJob.get();
        }
        else if (execute.getException() != null) {
          throw execute.getException();
        }
      } catch (Throwable e1) {
        UIUtil.fireNotification(NotificationType.ERROR,"Something Went Wrong ");
        LogHandler.error(e1.getMessage(), e1);

//        throw new RuntimeException(e1);
      }
      return null;
    }

  }
  public static String getUrlOutput(String lastApplyJobId) throws Exception {
    ResourceManagerClientProxy resourceManagerClientProxy = OracleCloudAccount.getInstance().getResourceManagerClientProxy();

    ListJobOutputCommand cmd = new ListJobOutputCommand(resourceManagerClientProxy, null, lastApplyJobId);
    ListJobOutputCommand.ListJobOutputResult result = cmd.execute();
    List<JobOutputSummary> outputSummaries = result.getOutputSummaries();
    Optional<JobOutputSummary> job = outputSummaries.stream().filter(p -> "app_url".equals(p.getOutputName())).findFirst();
    if (job.isPresent())
      return job.get().getOutputValue();
    else return "";
  }

  private JPopupMenu getStackSummaryActionMenu(StackSummary selectedSummary) {
    final JPopupMenu popupMenu = new JPopupMenu();
//
//    popupMenu.add(new JMenuItem(new RefreshAction(this, "Refresh")));
//    popupMenu.addSeparator();
//
    if (selectedSummary != null) {
      popupMenu.add(new JMenuItem(new LoadStackJobsAction(selectedSummary)));
      popupMenu.add(new JMenuItem(new LoadAppUrlAction(selectedSummary)));
      popupMenu.add(new JMenuItem(new ShowStackDetailsAction(selectedSummary)));

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

  public synchronized void populateTableData() {
//    ((DefaultTableModel) appStacksTable.getModel()).setRowCount(0);
    UIUtil.showInfoInStatusBar("Refreshing stack list .");

    refreshAppStackButton.setEnabled(false);

    final Runnable fetchData = () -> {
      try {
        String compartmentId = SystemPreferences.getCompartmentId();
        ListStackProxyCommand command =
                new ListStackProxyCommand(OracleCloudAccount.getInstance().getResourceManagerClientProxy(), compartmentId,jobsStackCache);
        ListStackProxyCommand.ListStackProxyResult result = (ListStackProxyCommand.ListStackProxyResult) commandStack.execute(command);
        if (!result.isError()) {
          appStackList =  result.getStacks();
        }
        else
        {
          throw new CommandFailedException("Failed refreshing list of stacks");
        }
      } catch (Exception exception) {
        //todo maybe we can change the values of profile,compartment, region to none for all dashboards
        appStackList = null;
        UIUtil.fireNotification(NotificationType.ERROR, exception.getMessage(), null);
//        LogHandler.error(exception.getMessage(), exception);
      }
    };

    final Runnable updateUI = () -> {
      filterStacks();
      refreshAppStackButton.setEnabled(true);
    };

    UIUtil.executeAndUpdateUIAsync(fetchData, updateUI);
  }

  private void populateTable(List<StackProxy> stackList) {
    if (stackList != null) {
      UIUtil.showInfoInStatusBar((stackList.size()) + " AppStack found.");
      final DefaultTableModel model = ((DefaultTableModel) appStacksTable.getModel());
      model.setRowCount(0);

      for (StackProxy stackProxy : stackList) {
        StackSummary s = stackProxy.getStackSummary();
        final Object[] rowData = new Object[AppStackTableModel.APPSTACK_COLUMN_NAMES.length];
//          final boolean isFreeTier =
//                  s.getIsFreeTier() != null && s.getIsFreeTier();
        rowData[0] = s.getDisplayName();
        rowData[1] = s.getDescription();
        rowData[2] = s.getTerraformVersion();
        rowData[3] = s.getLifecycleState();
        rowData[4] = s.getTimeCreated();
        rowData[5] =  stackProxy.getLastJob();
        model.addRow(rowData);
      }
    }
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
    LogHandler.info("AppStackDashboard: Handling the Event Update : " + propertyChangeEvent.toString());
    ((DefaultTableModel) appStacksTable.getModel()).setRowCount(0);

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
    // TODO: populateTableData();
//    UIUtil.invokeLater(()->{
    UIUtil.schedule(()->{
      populateTableData();
    });
//    });
  }

  private static class RefreshAction extends AbstractAction {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private final AppStackDashboard appStackDashBoard;

    public RefreshAction(AppStackDashboard adbDetails, String actionName) {
      super(actionName);
      this.appStackDashBoard = adbDetails;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
//      MyBackgroundTask myBackgroundTask = new MyBackgroundTask();
      UIUtil.schedule(()->{
        appStackDashBoard.populateTableData();
      });
    }
  }

  private static class CreateAction extends AbstractAction {
    /**
     *
     */
    private static final long serialVersionUID = 1L;
    private AppStackDashboard dashboard;

    public CreateAction(AppStackDashboard dashboard, String actionName) {
      super(actionName);
      this.dashboard = dashboard;
    }

    @Override
    public void actionPerformed(ActionEvent e) {

      AtomicReference<Map<String, String>> variables = new AtomicReference<>(new LinkedHashMap<>());

      dashboard.createAppStackButton.setEnabled(false);
      Runnable runnable = () -> {
        YamlLoader loader = new YamlLoader();
        dashboard.createAppStackButton.setEnabled(true);

        try {
          variables.set(loader.load());
        } catch (IntrospectionException | IllegalAccessException | InvocationTargetException ex) {
          throw new RuntimeException(ex);
        }
        if (variables.get() == null){
          dashboard.createAppStackButton.setEnabled(true);
          return;
        }



          Runnable creationRequest = ()->{
            try {
            ResourceManagerClientProxy proxy = OracleCloudAccount.getInstance().getResourceManagerClientProxy();

            String compartmentId = variables.get().get("appstack_compartment");
            ClassLoader cl = AppStackDashboard.class.getClassLoader();
            CreateStackCommand command =
                    new CreateStackCommand(proxy, compartmentId, cl, "appstackforjava.zip",loader.isApply());
            //          Map<String,String> variables = new ModelLoader().loadTestVariables();
            //          variables.put("shape","CI.Standard.E3.Flex");
            command.setVariables(variables.get());
            //          command.setVariables(variables.get());
            this.dashboard.commandStack.execute(command);
            } catch (Exception e1) {
              UIUtil.fireNotification(NotificationType.ERROR,"Something Went Wrong");
//          throw new RuntimeException(e1);
            }finally {
              dashboard.createAppStackButton.setEnabled(true);
            }
          };
          //todo just add the created one to the list

          Runnable updateUI = ()->{

            this.dashboard.populateTableData();
          };

          UIUtil.executeAndUpdateUIAsync(creationRequest,updateUI);


      };

      UIUtil.schedule(runnable);
    }
  }

  public static class ApplyAction extends AbstractAction {

    private static final long serialVersionUID = 7216149349340773007L;
    private final AppStackDashboard dashboard;
    public ApplyAction(AppStackDashboard dashboard, String title) {
      super("Apply");
      this.dashboard = dashboard;
    }


    @Override
    public void actionPerformed(ActionEvent e) {
      int selectedRow = this.dashboard.appStacksTable.getSelectedRow();
      // TODO: should be better way to get select row object
      if (selectedRow >=0 && selectedRow < this.dashboard.appStackList.size()) {
        this.dashboard.applyAppStackButton.setEnabled(false);
        StackProxy stackProxy = this.dashboard.appStackList.get(selectedRow);
        StackSummary stackSummary = stackProxy.getStackSummary();
        ResourceManagerClientProxy resourceManagerClient = OracleCloudAccount.getInstance().getResourceManagerClientProxy();

          UIUtil.schedule(()->{
            try {
              CreateJobResponse createApplyJobResponse = CreateStackCommand.createApplyJob(resourceManagerClient, stackSummary.getId(), stackSummary.getDisplayName());
              // invalidate cache because now we have new element in jobs of this stack
              jobsStackCache.remove(stackSummary.getId());

            }catch (JobRunningException | BmcException e1){
              UIUtil.fireNotification(NotificationType.ERROR,"Problem running stack apply:"+e1.getMessage());
            }finally {
              com.intellij.util.ui.UIUtil.invokeAndWaitIfNeeded((Runnable) ()->{
                dashboard.applyAppStackButton.setEnabled(true);
              });
            }
          });


//        String applyJobId = createApplyJobResponse.getJob().getId();

//        System.out.println(applyJobId);
//
//        // Get Job Terraform state GetJobTfStateRequest getJobTfStateRequest =
//        GetJobTfStateResponse jobTfState = resourceManagerClient.getJobTfState(applyJobId);
//        System.out.println(jobTfState.toString());
      }


    }
  }
  private static void invokeLater(AppStackDashboard appStackDashboard, AbstractBasicCommand command, JButton button){
    Thread t = new Thread(() -> {
      try {
        Result r = appStackDashboard.commandStack.execute(command);

      } catch (CommandFailedException e1) {
        UIUtil.fireNotification(NotificationType.ERROR,"Problem in apply:"+e1.getMessage());
      }finally {
        com.intellij.util.ui.UIUtil.invokeAndWaitIfNeeded((Runnable) ()->{
          button.setEnabled(true);
        });      }
    });
    t.start();
  }

  public static class DeleteAction extends AbstractAction {

    private static final long serialVersionUID = 7216149349340773007L;
    private AppStackDashboard dashboard;

    public DeleteAction(AppStackDashboard dashboard, String title) {
      super("Delete...");
      this.dashboard = dashboard;
    }
    
    private static class DeleteYesNoDialog extends DialogWrapper {
      // TODO: externalize
      private static final String DESTROY_ONLY_DESCRIPTION_TEXT = resBundle.getString("DESTROY_ONLY_DESCRIPTION_TEXT");
      private static final String DELETE_ONLY_DESCRIPTION_TEXT = resBundle.getString("DELETE_ONLY_DESCRIPTION_TEXT");
      private static final String DELETE_ALL_DESCRIPTION_TEXT = resBundle.getString("DELETE_ALL_DESCRIPTION_TEXT");
      JRadioButton deleteInfraStackRdoBtn;
      JRadioButton deleteStackRdoBtn;
      JRadioButton deleteAllRdoBtn;

      protected DeleteYesNoDialog() {
        super(true);
        init();
        setTitle("Confirm Delete");
        setOKButtonText("Ok");
      }

      @Override
      protected @Nullable JComponent createNorthPanel() {
        JPanel northPanel = new JPanel();
        JLabel label = new JLabel();
        label.setText("Warning: This operation cannot be undone. Are you sure?");
        northPanel.add(label);
        return northPanel;
      }


      @SuppressWarnings("serial")
      @Override
      protected void doOKAction() {
        showConfirmDialog();
      }

      private void showConfirmDialog() {
        JTextField inputField = new JTextField();
        Object[] message = {
                "This action cannot be undone. Please type \"confirm\" to confirm your choice:",
                inputField
        };
        int option = JOptionPane.showConfirmDialog(
                this.getContentPane(),
                message,
                "Confirm Deletion",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );
        if (option == JOptionPane.OK_OPTION) {
          String input = inputField.getText();
          if ("confirm".equalsIgnoreCase(input)) {
            JOptionPane.showMessageDialog(this.getContentPane(), "Deletion confirmed.", "Success", JOptionPane.INFORMATION_MESSAGE);
            super.doOKAction();
          } else {
            JOptionPane.showMessageDialog(this.getContentPane(), "Input is incorrect. Please type \"confirm\".", "Error", JOptionPane.ERROR_MESSAGE);
            showConfirmDialog(); // Show the dialog again for re-validation
          }
        }
      }

      @Override
      protected @Nullable JComponent createCenterPanel() {
        JPanel messagePanel = new JPanel();
        messagePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        BoxLayout mgr = new BoxLayout(messagePanel, BoxLayout.PAGE_AXIS);
        messagePanel.setLayout(mgr);

        JTextArea descriptionText = new JTextArea();
        descriptionText.setAlignmentX(Component.LEFT_ALIGNMENT);
        descriptionText.setEditable(false);
        descriptionText.setLineWrap(true);
        descriptionText.setWrapStyleWord(true);
        descriptionText.setColumns(20);
        descriptionText.setText(DESTROY_ONLY_DESCRIPTION_TEXT);

        JLabel selectOptionLbl = new JLabel("Select the deletion mode:");

        ActionListener selectionListener = new ActionListener() {

          @Override
          public void actionPerformed(ActionEvent e) {
            switch(e.getActionCommand()) {
            case "DESTROY_ONLY":
              descriptionText.setText(DESTROY_ONLY_DESCRIPTION_TEXT);
              break;
            case "DELETE_ONLY":
              descriptionText.setText(DELETE_ONLY_DESCRIPTION_TEXT);
              break;
            case "DELETE_ALL":
              descriptionText.setText(DELETE_ALL_DESCRIPTION_TEXT);
            }
          }
        };
        deleteInfraStackRdoBtn = new JRadioButton();
        initRadioBtn(deleteInfraStackRdoBtn, "DESTROY_ONLY", "Destroy the OCI resources for the selected stack", selectionListener
                     );

        deleteStackRdoBtn = new JRadioButton();
        initRadioBtn(deleteStackRdoBtn, "DELETE_ONLY", "Delete the definition for the stack", selectionListener);
        
        deleteAllRdoBtn = new JRadioButton();
        initRadioBtn(deleteAllRdoBtn, "DELETE_ALL", "Delete everything to do with the selected stack", selectionListener);

        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(deleteInfraStackRdoBtn);
        buttonGroup.add(deleteStackRdoBtn);
        buttonGroup.add(deleteAllRdoBtn);


        addHorizontalPadding(messagePanel, 15);
        messagePanel.add(descriptionText);
        addHorizontalPadding(messagePanel, 15);
        messagePanel.add(selectOptionLbl);
        //addHorizontalPadding(messagePanel, 10);
        messagePanel.add(deleteInfraStackRdoBtn);
        //addHorizontalPadding(messagePanel, 5);
        messagePanel.add(deleteStackRdoBtn);
        //addHorizontalPadding(messagePanel, 5);
        messagePanel.add(deleteAllRdoBtn);
       // pack();

       deleteInfraStackRdoBtn.setSelected(true);

        return messagePanel;
      }

      private void initRadioBtn(JRadioButton rdoButton, String actionCommand, String text,
                                ActionListener actionListener) {
        rdoButton.setText(text);
        rdoButton.setActionCommand(actionCommand);
        rdoButton.addActionListener(actionListener);
        rdoButton.setAlignmentX(Component.LEFT_ALIGNMENT);
      }

      private void addHorizontalPadding(JPanel messagePanel, int padding) {
        Dimension minSize = new Dimension(0, padding);
        Dimension prefSize = new Dimension(0, padding);
        Dimension maxSize = new Dimension(Short.MAX_VALUE, padding);
        messagePanel.add(new Box.Filler(minSize, prefSize, maxSize));
      }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
     
      int selectedRow = this.dashboard.appStacksTable.getSelectedRow();
      // TODO: should be better way to get select row object
      if (selectedRow >=0 && selectedRow < this.dashboard.appStackList.size()) {
        StackProxy stackProxy = this.dashboard.appStackList.get(selectedRow);
        StackSummary stackSummary = stackProxy.getStackSummary();
        ResourceManagerClientProxy proxy = OracleCloudAccount.getInstance().getResourceManagerClientProxy();
        DeleteYesNoDialog dialog = new DeleteYesNoDialog();
        //disable delete button
        dashboard.deleteAppStackButton.setEnabled(false);
        boolean yesToDelete = dialog.showAndGet();
        CompositeCommand compositeCommand;
        if (yesToDelete) {
          if (dialog.deleteInfraStackRdoBtn.isSelected()){
            DestroyStackCommand destroyCommand = new DestroyStackCommand(proxy, stackSummary.getId(),stackSummary.getDisplayName());
            compositeCommand = new CompositeCommand(destroyCommand);

          } else if (dialog.deleteAllRdoBtn.isSelected()) {
            DeleteAndDestroyCommand destroyCommand = new DeleteAndDestroyCommand(proxy, stackSummary.getId(),stackSummary.getDisplayName());
//            DeleteStackCommand deleteCommand = new DeleteStackCommand(proxy, stackSummary.getId(),stackSummary.getDisplayName());
            compositeCommand = new CompositeCommand(destroyCommand);
          } else if (dialog.deleteStackRdoBtn.isSelected()) {
            DeleteStackCommand deleteCommand = new DeleteStackCommand(proxy, stackSummary.getId(),stackSummary.getDisplayName());
            compositeCommand = new CompositeCommand(deleteCommand);
          } else {
              compositeCommand = null;
          }

            UIUtil.schedule(()->{
                try {
                  Result r = this.dashboard.commandStack.execute(compositeCommand);

                } catch (CommandFailedException e1) {
                  UIUtil.fireNotification(NotificationType.ERROR,"Problem with stack delete/destroy"+e1.getMessage());
                }finally {
                  com.intellij.util.ui.UIUtil.invokeAndWaitIfNeeded((Runnable) ()->{
                    dashboard.deleteAppStackButton.setEnabled(true);
                  });
                }
          });
          jobsStackCache.remove(stackSummary.getId());
//          invokeLater(this.dashboard,compositeCommand,dashboard.deleteAppStackButton);
        }else {
            compositeCommand = null;
            //enable  delete button in cancel case
          dashboard.deleteAppStackButton.setEnabled(true);
        }
      }
    }
  }
  public void triggerApplyAppStack() {
    if (applyAppStackButton != null) {
      SwingUtilities.invokeLater(() -> applyAppStackButton.doClick());
    }
  }
  @Override
  public String getTitle() {
    return "Stack";
  }


}
