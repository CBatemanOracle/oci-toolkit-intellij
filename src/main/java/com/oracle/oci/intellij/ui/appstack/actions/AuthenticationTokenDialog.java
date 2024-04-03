package com.oracle.oci.intellij.ui.appstack.actions;

import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.IconLoader;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextArea;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBDimension;
import com.oracle.bmc.identity.model.AuthToken;
import com.oracle.bmc.model.BmcException;
import com.oracle.oci.intellij.account.OracleCloudAccount;
import com.oracle.oci.intellij.ui.common.Icons;
import com.oracle.oci.intellij.ui.common.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

public class AuthenticationTokenDialog extends DialogWrapper {
    private JBTable tokensTable;
    private JPanel mainPanel;
    private List<AuthToken> tokens ;
    private Action deleteAction;
    private Action generateTokenAction ;

    protected AuthenticationTokenDialog( ) {
        super(true);

        setTitle("Auth Tokens");
        init();
        initDialog();
    }

    private void initDialog() {
        DefaultTableModel tokenModel = new DefaultTableModel();
        tokenModel.addColumn("Description");
        tokenModel.addColumn("Time Created");


        tokensTable.setModel(tokenModel);

        tokensTable.getSelectionModel().addListSelectionListener((e)->{
            updateActionState();
        });
        tokenModel.addTableModelListener(e -> {
//            System.out.println("Model changed!");
            updateActionState();
        });

        populateData();
        updateActionState();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return mainPanel;
    }

    private void  updateActionState(){
        boolean enableDeleteButton = tokensTable.getSelectedRow()!=-1 ;
        boolean enableGenerateButton = tokensTable.getRowCount()<2;
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

        generateTokenAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                generateToken();
                populateData();
            }
        };
        deleteAction.putValue("Name","Delete");

        generateTokenAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                generateToken();
                populateData();
            }
        };
        generateTokenAction.putValue("Name","Generate");

//        updateActionState();
        getCancelAction().putValue("Name","Close");
        return new Action[]{deleteAction,generateTokenAction,getCancelAction()};

    }

    private void generateToken() {
        // verify if the user have just two tokens ....
// c
        if (tokens.size()<=2){
            CreateAuthDialog createAuthDialog = new CreateAuthDialog();
            createAuthDialog.show();
//            if (createAuthDialog.showAndGet()){


//            }
        }else {
            UIUtil.fireNotification(NotificationType.ERROR,"you can just have two tokens at a time ",null);
        }
    }

    private void populateData() {
        ((DefaultTableModel) tokensTable.getModel()).setRowCount(0);

        generateTokenAction.setEnabled(false);
        deleteAction.setEnabled(false);

        Runnable fetchData = ()->{
            try {
                OracleCloudAccount.IdentityClientProxy identityClientProxy = OracleCloudAccount.getInstance().getIdentityClient();
                tokens = identityClientProxy.getAuthTokenList();
            }catch(RuntimeException ex){
                UIUtil.fireNotification(NotificationType.ERROR, ex.getMessage(), null);
            }
        };

        Runnable updateUI = ()->{
            final DefaultTableModel model = (DefaultTableModel) tokensTable.getModel();
            model.setRowCount(0);

            List<Object> row = new ArrayList<>();
            this.tokens.forEach(token->{
                row.add(token.getDescription());
                row.add(token.getTimeCreated());
                model.addRow(row.toArray());
                row.clear();
            });
            updateActionState();
//            repaint();


        };

//        ApplicationManager.getApplication().executeOnPooledThread(()->{
//            fetchData.run();
//            ApplicationManager.getApplication().invokeLater(updateUI, ModalityState.any());
//            });

        UIUtil.executeAndUpdateUIAsync(fetchData, updateUI, ModalityState.any());
    }

    private void deleteToken(String id) {
        try{
            OracleCloudAccount.getInstance().getIdentityClient().deleteAuthToken(id);
        }catch (BmcException ex){
            String simplifiedMessage = ex.getMessage();
            if (ex.getStatusCode() == 403)
             simplifiedMessage = "Error returned by DeleteAuthToken operation in Identity service.(403, NotAllowed, false) Please go to your home region PHX to execute CREATE, UPDATE and DELETE operations. ";
//            ex.
            setErrorText(simplifiedMessage);
           UIUtil.fireNotification(NotificationType.ERROR,simplifiedMessage,null);
        }
    }

    static class CreateAuthDialog extends DialogWrapper{
        private JBTextArea descriptionTextField;

        protected CreateAuthDialog() {
            super(true);
            init();
            setTitle("Create New Auth Token ");
            setOKButtonText("Create");
        }
        public String getAuthDescription(){
            return descriptionTextField.getText();
        }

        @Override
        protected void doOKAction() {
            getOKAction().setEnabled(false);
            String description = descriptionTextField.getText();
            String message = "" ;
            try {
                AuthToken authToken = OracleCloudAccount.getInstance().getIdentityClient().generateToken(description);
                 message= authToken.getToken();

            }catch (RuntimeException ex){
                UIUtil.fireNotification(NotificationType.ERROR, ex.getMessage(), null);
//                System.out.println("there is an error here "+ex);
            }
//            AuthToken authToken = OracleCloudAccount.getInstance().getIdentityClient().generateToken(description);
            JTextField textField = new JTextField(message);
            textField.setEditable(false);
            ShowTokenSecretDialog dialog = new ShowTokenSecretDialog(message);
            dialog.show();
//            JOptionPane.showMessageDialog(null, textField, "Copy Text", JOptionPane.INFORMATION_MESSAGE);
            getOKAction().setEnabled(true);

            super.doOKAction();
        }

        @Override
        protected @Nullable JComponent createCenterPanel() {
            BorderLayout borderLayout = new BorderLayout();
            borderLayout.setHgap(9);
            JPanel createPanel = new JPanel(borderLayout);
            createPanel.setPreferredSize(new JBDimension(450,80));

            JBLabel descriptionLabel = new JBLabel("Description");

            descriptionTextField = new JBTextArea();
            descriptionTextField.setColumns(20);
            descriptionTextField.setRows(2);
            descriptionTextField.setLineWrap(true);
            descriptionTextField.setMargin(new Insets(6,2,2,2));
            descriptionTextField.setFont(descriptionTextField.getFont().deriveFont(descriptionTextField.getFont().getSize() - 2f));

            createPanel.add(descriptionLabel,BorderLayout.WEST);
            createPanel.add(descriptionTextField,BorderLayout.CENTER);
            return createPanel;
        }
    }

    static class ShowTokenSecretDialog extends DialogWrapper{

        private JPanel mainPanel;
        private JBTextField tokenSecret;
        private JButton copyButton;

        protected ShowTokenSecretDialog(String tokenSecret) {
            super(true);
            this.tokenSecret.setText(tokenSecret);
//            this.tokenSecret.setBorder(null);
//            this.tokenSecret.setEnabled(false);
            setTitle("Token");
            init();
            initCopyButton();
        }

        private void initCopyButton() {
            String copyPath = Icons.COPY.getPath();

            // Create the button and set the icon
//            JButton copyButton = new JButton();
            copyButton.setIcon(IconLoader.getIcon(copyPath));
            copyButton.setText("");
            copyButton.setBackground(null);
            copyButton.setPreferredSize(new JBDimension(20,20));
            copyButton.setBorder(null);
            copyButton.setOpaque(false);
            copyButton.setContentAreaFilled(false);
            String finalValue = tokenSecret.getText();
            copyButton.addActionListener(e -> {
                String textToCopy = finalValue;  // Replace with the actual text you want to copy
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                StringSelection selection = new StringSelection(textToCopy);
                clipboard.setContents(selection, selection);
                copyButton.setIcon(null);
                copyButton.setToolTipText("Copy");
                copyButton.setOpaque(false);
                copyButton.setContentAreaFilled(false); //
//                        copyButton.setText("copied");
                // notify
                new Timer(500,ev->{
                    copyButton.setIcon(IconLoader.getIcon(copyPath));
                }).start();
            });
        }

        @Override
        protected @Nullable JComponent createCenterPanel() {
            return mainPanel;
        }

        @Override
        protected Action @NotNull [] createActions() {
            getOKAction().putValue("Name","Close");
            return new Action[]{getOKAction()};
        }
    }
}
