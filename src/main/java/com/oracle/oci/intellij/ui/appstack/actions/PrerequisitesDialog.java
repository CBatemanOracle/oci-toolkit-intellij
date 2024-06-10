package com.oracle.oci.intellij.ui.appstack.actions;

import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.ResourceBundle;
import java.util.StringTokenizer;

public class PrerequisitesDialog extends DialogWrapper {
    JPanel mainPanel ;
    private JBList<String> prerequisitesList;
    DefaultListModel<String> prerequisitesListModel;
    ResourceBundle res = ResourceBundle.getBundle("appStackWizard");


    public PrerequisitesDialog() {
        super(false);
        prerequisitesListModel = new DefaultListModel<>();
        prerequisitesList.setModel(prerequisitesListModel);
        prerequisitesList.setCellRenderer(new ToolTipCellListRenderer());
        loadItemsFromResourceBundle();
        init();
    }

    private void loadItemsFromResourceBundle() {
        String items = res.getString("prerequisites");
        if (items!= null && !items.isEmpty()){
            StringTokenizer tokenizer = new StringTokenizer(items,",");
            while(tokenizer.hasMoreTokens()){
                prerequisitesListModel.addElement(tokenizer.nextToken().trim());
            }
        }
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return mainPanel;
    }


    static class ToolTipCellListRenderer extends DefaultListCellRenderer{
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {

            Component component =  super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (component instanceof JLabel){
                JLabel label = (JLabel)component;
                label.setToolTipText("<html> Tooltip for  A Java application in an OCI DevOps project <br>" +
                                             "(can be a mirror of an existing GitHub repo).This isn't  <br> " +
                                             "required if the application is provided as a container image. </html>"); // Set tooltip text
            }
            return component;
        }
    }
}
