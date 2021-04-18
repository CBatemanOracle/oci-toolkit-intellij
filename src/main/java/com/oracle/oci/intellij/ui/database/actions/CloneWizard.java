/**
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

package com.oracle.oci.intellij.ui.database.actions;

import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.oracle.bmc.database.model.AutonomousDatabaseSummary;
import com.oracle.bmc.database.model.CreateAutonomousDatabaseBase;
import com.oracle.bmc.database.model.CreateAutonomousDatabaseCloneDetails;
import com.oracle.bmc.database.model.CreateAutonomousDatabaseDetails;
import com.oracle.bmc.identity.model.Compartment;
import com.oracle.oci.intellij.account.Identity;
import com.oracle.oci.intellij.account.ServicePreferences;
import com.oracle.oci.intellij.ui.common.CompartmentSelection;
import com.oracle.oci.intellij.ui.common.UIUtil;
import com.oracle.oci.intellij.ui.database.ADBConstants;
import com.oracle.oci.intellij.ui.database.ADBInstanceClient;
import org.jetbrains.annotations.Nullable;
import com.oracle.bmc.database.model.CreateAutonomousDatabaseCloneDetails.CloneType;

import javax.swing.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

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
  private JTextField compartmentCmb;
  private JTextField dbNameTxt;
  private JCheckBox alwaysFreeChk;
  private JSpinner cpuCountSpnr;
  private JSpinner storageSpnr;
  private JCheckBox autoScalingChk;
  private JTextField userNameTxt;
  private JPasswordField passwordTxt;
  private JPasswordField confirmPasswordTxt;
  private JRadioButton byolRBtn;
  private JRadioButton licenseIncldBtn;
  private JPanel licenseTypePanel;
  private JLabel pwdInstrLbl;
  private JButton compartmentBtn;
  private ButtonGroup cloneTypeGroup;
  private ButtonGroup licenseTypeGrp;

  private final AutonomousDatabaseSummary autonomousDatabaseSummary;
  private Compartment selectedCompartment;

  protected CloneWizard(AutonomousDatabaseSummary autonomousDatabaseSummary) {
    super(true);
    if(autonomousDatabaseSummary == null)
      throw new RuntimeException("ADB instance is NULL");
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
    orginDBName.setEditable(false);
    displayNameTxt.setText("Clone of " + autonomousDatabaseSummary.getDbName());
    cpuCountSpnr.setModel(new SpinnerNumberModel(
        autonomousDatabaseSummary.getCpuCoreCount().intValue(),
        ADBConstants.CPU_CORE_COUNT_MIN, ADBConstants.CPU_CORE_COUNT_MAX,
        ADBConstants.CPU_CORE_COUNT_INCREMENT));
    storageSpnr.setModel(new SpinnerNumberModel(
        autonomousDatabaseSummary.getDataStorageSizeInTBs().intValue(),
        ADBConstants.STORAGE_IN_TB_MIN, ADBConstants.STORAGE_IN_TB_MAX,
        ADBConstants.STORAGE_IN_TB_INCREMENT));
    pwdInstrLbl.setText("<html>" + PASSWORD_TOOlTIP + "</html>");
    userNameTxt.setText(DEFAULT_USERNAME);
    userNameTxt.setEditable(false);
    passwordTxt.setToolTipText(PASSWORD_TOOlTIP);

    if (autonomousDatabaseSummary.getLicenseModel()
        .equals(AutonomousDatabaseSummary.LicenseModel.BringYourOwnLicense))
      byolRBtn.setSelected(true);
    else
      licenseIncldBtn.setSelected(true);

    compartmentCmb.setEditable(false);

    selectedCompartment = Identity.getInstance().getRootCompartment();
    if(selectedCompartment != null)
      compartmentCmb.setText(selectedCompartment.getName());
    else
      compartmentCmb.setText("Select Compartment");


    compartmentBtn.addActionListener((e) -> {
      CompartmentSelection compartmentSelection = CompartmentSelection.newInstance();

      if (compartmentSelection.showAndGet()) {
        selectedCompartment = compartmentSelection.getSelectedCompartment();

        if (selectedCompartment != null) {
          compartmentCmb.setText(selectedCompartment.getName());
        }
      }
    });

    alwaysFreeChk.addChangeListener((e) -> {
      if (alwaysFreeChk.isSelected()) {
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

    if(autonomousDatabaseSummary.getDbWorkload()
        == AutonomousDatabaseSummary.DbWorkload.Ajd) {
      licenseIncldBtn.setSelected(true);
      licenseTypePanel.setEnabled(false);
      alwaysFreeChk.setEnabled(false);
      alwaysFreeChk.setSelected(false);
      licenseIncldBtn.setSelected(true);
      licenseIncldBtn.setEnabled(false);
      byolRBtn.setEnabled(false);
    }
  }

  public void doOKAction() {
    if(selectedCompartment == null) {
      Messages.showErrorDialog("Invalid Compartment value", "Error");
      return;
    }

    if (!isValidPassword()) {
      return;
    }

    final String compartmentId = selectedCompartment.getId();
    final CreateAutonomousDatabaseDetails.DbWorkload workloadType;

    if (autonomousDatabaseSummary.getDbWorkload()
        == AutonomousDatabaseSummary.DbWorkload.Dw) {
      workloadType = CreateAutonomousDatabaseDetails.DbWorkload.Dw;
    } else if (autonomousDatabaseSummary.getDbWorkload()
        == AutonomousDatabaseSummary.DbWorkload.Ajd) {
      workloadType = CreateAutonomousDatabaseDetails.DbWorkload.Ajd;
    } else {
      workloadType = CreateAutonomousDatabaseDetails.DbWorkload.Oltp;
    }

    final boolean isFreeTier =
        (autonomousDatabaseSummary.getIsFreeTier() != null)
            && autonomousDatabaseSummary.getIsFreeTier() && alwaysFreeChk
            .isSelected();
    final String storage = alwaysFreeChk.isSelected() ?
        ADBConstants.ALWAYS_FREE_STORAGE_TB_DUMMY :
        storageSpnr.getValue().toString();

    final char[] pswd = passwordTxt.getPassword();
    final CreateAutonomousDatabaseCloneDetails cloneRequest = CreateAutonomousDatabaseCloneDetails
        .builder().compartmentId(compartmentId)
        .cpuCoreCount(Integer.valueOf(cpuCountSpnr.getValue().toString()))
        .dataStorageSizeInTBs(Integer.valueOf(storage))
        .displayName(displayNameTxt.getText().trim())
        .adminPassword(new String(pswd))
        .dbName(dbNameTxt.getText()).dbWorkload(workloadType)
        .isAutoScalingEnabled(autoScalingChk.isSelected())
        .licenseModel(getLicenseModel())
        .sourceId(autonomousDatabaseSummary.getId()).cloneType(getCloneType())
        .isFreeTier(isFreeTier).build();

    final Runnable nonblockingUpdate = () -> {
      try {
        ADBInstanceClient.getInstance().createClone(cloneRequest);
        ApplicationManager.getApplication().invokeLater(() -> {
          UIUtil.fireNotification(NotificationType.INFORMATION,"ADB Instance cloned successfully.");
          ServicePreferences.fireADBInstanceUpdateEvent("Clone");
        });
      }
      catch (Exception e) {
        ApplicationManager.getApplication().invokeLater(
            () -> UIUtil.fireNotification(NotificationType.ERROR, "Failed to clone ADB Instance : " + e.getMessage()));
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
    final char[] adminPassword = passwordTxt.getPassword();
    final char[] confirmAdminPassword = confirmPasswordTxt.getPassword();

    if (adminPassword == null || adminPassword.length == 0) {
      Messages.showErrorDialog("Admin password cannot be empty.", "Error");
      return false;
    }
    else if (!Arrays.equals(adminPassword, confirmAdminPassword)) {
      Messages.showErrorDialog("Confirm Admin password must match Admin password", "Error");
      return false;
    }
    Arrays.fill(adminPassword,' ');
    Arrays.fill(confirmAdminPassword,' ');
    return true;
  }

  @Nullable
  @Override
  protected JComponent createCenterPanel() {
    mainPanel.setPreferredSize(new Dimension(475,500));
    return mainPanel;
  }
}
