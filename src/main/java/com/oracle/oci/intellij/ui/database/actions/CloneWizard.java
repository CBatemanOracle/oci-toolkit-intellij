package com.oracle.oci.intellij.ui.database.actions;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.oracle.bmc.database.model.AutonomousDatabaseSummary;
import com.oracle.bmc.database.model.CreateAutonomousDatabaseBase;
import com.oracle.bmc.database.model.CreateAutonomousDatabaseCloneDetails;
import com.oracle.bmc.database.model.CreateAutonomousDatabaseDetails;
import com.oracle.oci.intellij.ui.common.CompartmentSelection;
import com.oracle.oci.intellij.ui.common.UIUtil;
import com.oracle.oci.intellij.ui.database.ADBConstants;
import com.oracle.oci.intellij.ui.database.ADBInstanceClient;
import org.jetbrains.annotations.Nullable;
import com.oracle.bmc.database.model.CreateAutonomousDatabaseCloneDetails.CloneType;

import javax.swing.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

public class CloneWizard extends DialogWrapper {

  private static final String PASSWORD_TOOlTIP =
      "Password must be 12 to 30 characters and contain at least one uppercase letter,\n"
          + " one lowercase letter, and one number. The password cannot contain the double \n"
          + "quote (\") character or the username \"admin\".";
  private static final String DEFAULT_USERNAME = "ADMIN";

  private JPanel mainPanel;
  private JPanel topPanel;
  private JPanel bottomPanel;
  private JPanel centerPanel;
  private JRadioButton fullCloneRadioButton;
  private JRadioButton metadataCloneRadioButton;
  private JTextField orginDBName;
  private JTextField displayNameTxt;
  private TextFieldWithBrowseButton compartmentCmb;
  private JTextField dbNameTxt;
  private JCheckBox alwayFreeChk;
  private JSpinner cpuCountSpnr;
  private JSpinner storageSpnr;
  private JCheckBox autoScalingChk;
  private JTextField userNameTxt;
  private JPasswordField passwordTxt;
  private JPasswordField confirmPasswordTxt;
  private JRadioButton byolRBtn;
  private JRadioButton licenseIncldBtn;
  private JPanel licenseTypePanel;
  private ButtonGroup cloneTypeGroup;

  private ButtonGroup licenseTypeGrp;

  private Map<String, String> compartmentMap = new LinkedHashMap<String, String>();

  private final AutonomousDatabaseSummary autonomousDatabaseSummary;
  private CompartmentSelection compartmentSelection;

  protected CloneWizard(AutonomousDatabaseSummary autonomousDatabaseSummary) {
    super(true);
    this.autonomousDatabaseSummary = autonomousDatabaseSummary;
    setTitle("Create ADB Instance");
    setOKButtonText("Clone");
    licenseTypeGrp = new ButtonGroup();
    cloneTypeGroup = new ButtonGroup();
    licenseTypeGrp.add(byolRBtn);
    licenseTypeGrp.add(licenseIncldBtn);
    cloneTypeGroup.add(fullCloneRadioButton);
    cloneTypeGroup.add(metadataCloneRadioButton);

    init();
    fullCloneRadioButton.setSelected(true);
    final SimpleDateFormat DATE_TIME_FORMAT = new SimpleDateFormat(
        "yyyyMMddHHmm");
    final String defaultDBName = "DB" + DATE_TIME_FORMAT.format(new Date());
    dbNameTxt.setText(defaultDBName);
    orginDBName.setText(autonomousDatabaseSummary.getDbName());
    displayNameTxt.setText("Clone of " + autonomousDatabaseSummary.getDbName());
    cpuCountSpnr.setModel(new SpinnerNumberModel(
        autonomousDatabaseSummary.getCpuCoreCount().intValue(),
        ADBConstants.CPU_CORE_COUNT_MIN, ADBConstants.CPU_CORE_COUNT_MAX,
        ADBConstants.CPU_CORE_COUNT_INCREMENT));
    storageSpnr.setModel(new SpinnerNumberModel(
        autonomousDatabaseSummary.getDataStorageSizeInTBs().intValue(),
        ADBConstants.STORAGE_IN_TB_MIN, ADBConstants.STORAGE_IN_TB_MAX,
        ADBConstants.STORAGE_IN_TB_INCREMENT));

    userNameTxt.setText(DEFAULT_USERNAME);
    userNameTxt.setEditable(false);
    passwordTxt.setToolTipText(PASSWORD_TOOlTIP);

    if (autonomousDatabaseSummary.getLicenseModel()
        .equals(AutonomousDatabaseSummary.LicenseModel.BringYourOwnLicense))
      byolRBtn.setSelected(true);
    else
      licenseIncldBtn.setSelected(true);

    compartmentCmb.setEditable(false);
    compartmentSelection = new CompartmentSelection();
    compartmentCmb.addActionListener((e) -> {
      compartmentSelection.showAndGet();
      compartmentCmb
          .setText(compartmentSelection.getSelectedCompartment().getName());

    });

    alwayFreeChk.addChangeListener((e) -> {
      if (alwayFreeChk.isSelected()) {
        cpuCountSpnr.setValue(1);
        cpuCountSpnr.setEnabled(false);
        storageSpnr.setValue(0.02);
        storageSpnr.setEnabled(false);
        licenseIncldBtn.setSelected(true);
        byolRBtn.setEnabled(false);
        licenseIncldBtn.setEnabled(false);

      }
      else {
        cpuCountSpnr.setValue(1);
        cpuCountSpnr.setEnabled(true);
        storageSpnr.setValue(1);
        storageSpnr.setEnabled(true);
        byolRBtn.setSelected(true);
        byolRBtn.setEnabled(true);
        licenseIncldBtn.setEnabled(true);

      }
    });
  }

  public void doOKAction() {
    if (!isValidPassword())
      return;

    final String compartmentId = compartmentSelection.getSelectedCompartment()
        .getCompartmentId();
    final CreateAutonomousDatabaseDetails.DbWorkload workloadType;
    if (autonomousDatabaseSummary.getDbWorkload()
        == AutonomousDatabaseSummary.DbWorkload.Dw) {
      workloadType = CreateAutonomousDatabaseDetails.DbWorkload.Dw;
    }
    else {
      workloadType = CreateAutonomousDatabaseDetails.DbWorkload.Oltp;
    }

    final boolean isFreeTier =
        (autonomousDatabaseSummary.getIsFreeTier() != null)
            && autonomousDatabaseSummary.getIsFreeTier() && alwayFreeChk
            .isSelected();
    final String storage = alwayFreeChk.isSelected() ?
        ADBConstants.ALWAYS_FREE_STORAGE_TB_DUMMY :
        storageSpnr.getValue().toString();

    final CreateAutonomousDatabaseCloneDetails cloneRequest = CreateAutonomousDatabaseCloneDetails
        .builder().compartmentId(compartmentId)
        .cpuCoreCount(Integer.valueOf(cpuCountSpnr.getValue().toString()))
        .dataStorageSizeInTBs(Integer.valueOf(storage))
        .displayName(displayNameTxt.getText().trim())
        .adminPassword(new String(passwordTxt.getPassword()))
        .dbName(dbNameTxt.getText()).dbWorkload(workloadType)
        .isAutoScalingEnabled(autoScalingChk.isSelected())
        .licenseModel(getLicenseModel())
        .sourceId(autonomousDatabaseSummary.getId()).cloneType(getCloneType())
        .isFreeTier(isFreeTier).build();

    final Runnable nonblockingUpdate = () -> {
      try {
        ADBInstanceClient.getInstance().createClone(cloneRequest);
        ApplicationManager.getApplication().invokeLater(() -> UIUtil
            .fireSuccessNotification("ADB Instance cloned successfully."));
      }
      catch (Exception e) {
        ApplicationManager.getApplication().invokeLater(
            () -> UIUtil.fireErrorNotification("ADB Instance clone failed."));
      }
    };

    // Do this in background
    UIUtil.fetchAndUpdateUI(nonblockingUpdate, null);

    close(DialogWrapper.OK_EXIT_CODE);
  }

  public CloneType getCloneType() {
    if (fullCloneRadioButton.isSelected()) {
      return CloneType.Full;
    }
    else {
      return CloneType.Metadata;
    }
  }

  private CreateAutonomousDatabaseBase.LicenseModel getLicenseModel() {
    if (licenseIncldBtn.isSelected()) {
      return CreateAutonomousDatabaseBase.LicenseModel.LicenseIncluded;
    }
    else {
      return CreateAutonomousDatabaseBase.LicenseModel.BringYourOwnLicense;
    }
  }

  private boolean isValidPassword() {
    final String adminPassword = new String(passwordTxt.getPassword());
    final String confirmAdminPassword = new String(
        confirmPasswordTxt.getPassword());

    if (adminPassword == null || adminPassword.trim().equals("")) {
      Messages.showErrorDialog("Admin password cannot be empty.", "Error");
      return false;

    }
    else if (!adminPassword.equals(confirmAdminPassword)) {
      Messages
          .showErrorDialog("Confirm Admin password must match Admin password",
              "Error");
      return false;
    }
    return true;
  }

  @Nullable @Override protected JComponent createCenterPanel() {
    return mainPanel;
  }
}
