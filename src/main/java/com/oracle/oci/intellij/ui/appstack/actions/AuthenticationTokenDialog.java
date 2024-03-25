package com.oracle.oci.intellij.ui.appstack.actions;

import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.table.JBTable;
import com.oracle.bmc.identity.model.AuthToken;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

public class AuthenticationTokenDialog extends DialogWrapper {
    private JBTable tokensTable;
    private JPanel mainPanel;
    private final List<AuthToken> tokens ;
    private Action deleteAction;
    private Action generateTokenAction ;

    protected AuthenticationTokenDialog( List<AuthToken> tokens) {
        super(true);
        this.tokens = tokens;
        init();
        setTitle("Auth Tokens");
        setOKButtonText("Close");
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {

        DefaultTableModel tokenModel = new DefaultTableModel();
        tokenModel.addColumn("Description");
        tokenModel.addColumn("Time Created");
        List<Object> row = new ArrayList<>();
        this.tokens.forEach(token->{
            row.add(token.getDescription());
            row.add(token.getTimeCreated());
            tokenModel.addRow(row.toArray());
            row.clear();
        });

        tokensTable.setModel(tokenModel);

        tokensTable.getSelectionModel().addListSelectionListener((e)->{
            updateActionState();
        });

        return mainPanel;
    }

    private void  updateActionState(){
        boolean enableDeleteButton = tokensTable.getSelectedRow()!=-1 ;
        boolean enableGenerateButton = tokensTable.getRowCount()<=2;
        System.out.println("this is length "+tokensTable.getRowCount());
        deleteAction.setEnabled(enableDeleteButton);
        generateTokenAction.setEnabled(enableGenerateButton);
    }

    @Override
    protected Action @NotNull [] createActions() {
         deleteAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int selectedRow = tokensTable.getSelectedRow() ;

                if (selectedRow != -1){
                    AuthToken selectedToken = tokens.get(selectedRow);
                    deleteToken(selectedToken.getId());
                    populateData();
                }
            }
        };
        deleteAction.putValue("Name","Delete");

        generateTokenAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                generateToken();
            }
        };
        generateTokenAction.putValue("Name","Generate");

        updateActionState();
        getCancelAction().putValue("Name","Close");
        return new Action[]{deleteAction,generateTokenAction,getCancelAction()};

    }

    private void generateToken() {

        // verify if the user have just two tokens ....

    }

    private void populateData() {
    }

    private void deleteToken(String id) {
        System.out.println("delete item");
    }
}
