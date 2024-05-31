package com.oracle.oci.intellij.ui.common;

import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.IconLoader;
import com.intellij.ui.components.ActionLink;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.JBDimension;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * InformationDialog is a utility dialog class used to display a simple information message to the user.
 * This class extends {@link DialogWrapper}, making it easy to integrate with the IntelliJ Platform UI.
 * The dialog is modal and provides an "OK" button to dismiss it.
 *
 * Usage:
 * This dialog is used to provide users with information messages, such as confirmations, notifications,
 * or other read-only text relevant to current operations.
 */
public class MessageDialog extends DialogWrapper {
    private JBLabel informationTextArea;
    private JPanel mainPanel;
    private JLabel typeIcon;
    private ActionLink documentationLink;
    private JPanel linkPanel;

    public enum MessageType{
        ERROR(Icons.ERROR.getPath()),
        INFO(Icons.INFO.getPath()),
        WARN(Icons.WARN.getPath());

        public final String label;

        MessageType(String label) {
            this.label = label;
        }
    }

    public MessageDialog(String information,MessageType messageType,String url) {
        super(false);
       // TODO CREATE METHOD TO UPLOAD ICONS PROPERLY
        typeIcon.setIcon(IconLoader.getIcon(messageType.label));
        informationTextArea.setText(information);
        if (url!= null){
            AbstractAction abstractAction =new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    UIUtil.createWebLink(documentationLink,url);
                }
            };
            this.documentationLink.setAction(abstractAction);
            this.documentationLink.setText("Documentation");
            linkPanel.setVisible(true);
        }

        init();
    }

    @Override
    protected Action @NotNull [] createActions() {
        return new Action[]{getOKAction()};
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        mainPanel.setPreferredSize(new JBDimension(400,50));
        return mainPanel;
    }
}
