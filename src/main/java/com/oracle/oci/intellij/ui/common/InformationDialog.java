package com.oracle.oci.intellij.ui.common;

import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.util.ui.JBDimension;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
/**
 * InformationDialog is a utility dialog class used to display a simple information message to the user.
 * This class extends {@link DialogWrapper}, making it easy to integrate with the IntelliJ Platform UI.
 * The dialog is modal and provides an "OK" button to dismiss it.
 *
 * Usage:
 * This dialog is used to provide users with information messages, such as confirmations, notifications,
 * or other read-only text relevant to current operations.
 */
public class InformationDialog extends DialogWrapper {
    private JTextArea informationTextArea;
    private JPanel mainPanel;

    public InformationDialog(String information ) {
        super(false);

        informationTextArea.setText(information);
        init();
    }

    @Override
    protected Action @NotNull [] createActions() {
        return new Action[]{getOKAction()};
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        mainPanel.setPreferredSize(new JBDimension(400,200));
        return mainPanel;
    }
}
