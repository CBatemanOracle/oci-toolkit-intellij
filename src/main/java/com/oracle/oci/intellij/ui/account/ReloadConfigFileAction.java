package com.oracle.oci.intellij.ui.account;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.util.IconLoader;
import com.oracle.oci.intellij.account.OracleCloudAccount;
import com.oracle.oci.intellij.account.SystemPreferences;
import com.oracle.oci.intellij.ui.common.Icons;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class ReloadConfigFileAction extends AnAction {
    public ReloadConfigFileAction() {
        super("Reload File Configuration", "Reload file configuration",
                IconLoader.getIcon(Icons.RELOAD.getPath()));
    }
    public void actionPerformed(@NotNull AnActionEvent event) {
        String configFile = SystemPreferences.getConfigFilePath();
        String profileName = SystemPreferences.getProfileName();
        try {
            OracleCloudAccount.getInstance().configure(configFile, profileName);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

}

