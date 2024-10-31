package com.oracle.oci.intellij.api.services;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.swing.JOptionPane;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.oracle.oci.intellij.api.ext.ContributeADBActions;
import com.oracle.oci.intellij.api.ext.ContributeADBActions.ExtensionContextAction;
import com.oracle.oci.intellij.api.ext.UIModelContext;
import com.oracle.oci.intellij.api.oci.OCIDatabase;

@Service
public final class DatabaseContextMenuExtension {

    private static final Logger LOG = Logger.getInstance(DatabaseContextMenuExtension.class);

    private static final String COM_ORACLE_OCI_INTELLIJ_PLUGIN_CONTRIBUTE_ADB_ACTION = 
            "com.oracle.ocidbtest.contributeADBAction";
    private static final ExtensionPointName<ContributeADBActions> EP_NAME = ExtensionPointName
            .create(COM_ORACLE_OCI_INTELLIJ_PLUGIN_CONTRIBUTE_ADB_ACTION);

    public List<ContributeADBActions.ExtensionContextAction> getActions(UIModelContext context) {
        LOG.info("useRegisteredExtensions");
        List<ExtensionContextAction> dbs = new ArrayList<>();
        for (ContributeADBActions extension : EP_NAME.getExtensionList()) {
            dbs.addAll(extension.getModelContextActions(context));
        }
        
        Optional.ofNullable(System.getProperties().get("idea.plugin.in.sandbox.mode")).ifPresent(prop -> {
          if (Boolean.parseBoolean(prop.toString())) {
            dbs.add(new ExtensionContextAction("Test Extension (runIde only)") {

              @Override
              public void actionPerformed(ActionEvent e) {
                OCIDatabase contextObject = (OCIDatabase) context.getContextObject();
                JOptionPane.showMessageDialog(null, contextObject.getDisplayName());
              }
            });
          }
        });
        return dbs;
    }
}
