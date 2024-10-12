package com.oracle.oci.intellij.api.ext;

import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.AbstractAction;

import com.intellij.openapi.extensions.PluginAware;

public interface ContributeADBActions extends PluginAware {

    List<ExtensionContextAction>  getModelContextActions(UIModelContext context);

    
    abstract class ExtensionContextAction extends AbstractAction {

        //private String displayName;

        public ExtensionContextAction() {
            this("Unset Display Name");
        }
        public ExtensionContextAction(String displayName) {
            super(displayName);
        }

        
        @Override
        public void actionPerformed(ActionEvent e) {
            // TODO Auto-generated method stub
            
        }

    }

}
