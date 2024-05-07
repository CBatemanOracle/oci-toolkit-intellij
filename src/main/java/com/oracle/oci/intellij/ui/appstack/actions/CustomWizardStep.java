package com.oracle.oci.intellij.ui.appstack.actions;

import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBPasswordField;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.wizard.WizardModel;
import com.intellij.ui.wizard.WizardNavigationState;
import com.intellij.ui.wizard.WizardStep;
import com.intellij.util.ui.JBDimension;
import com.oracle.bmc.core.model.Subnet;
import com.oracle.bmc.core.model.Vcn;
import com.oracle.bmc.database.model.AutonomousDatabaseSummary;
import com.oracle.bmc.devops.model.RepositoryBranchSummary;
import com.oracle.bmc.devops.model.RepositorySummary;
import com.oracle.bmc.dns.model.ZoneSummary;
import com.oracle.bmc.http.client.internal.ExplicitlySetBmcModel;
import com.oracle.bmc.identity.model.AuthToken;
import com.oracle.bmc.identity.model.AvailabilityDomain;
import com.oracle.bmc.identity.model.Compartment;
import com.oracle.bmc.keymanagement.model.KeySummary;
import com.oracle.bmc.keymanagement.model.VaultSummary;
import com.oracle.oci.intellij.account.OracleCloudAccount;
import com.oracle.oci.intellij.ui.appstack.models.Controller;
import com.oracle.oci.intellij.ui.appstack.models.Utils;
import com.oracle.oci.intellij.ui.appstack.models.Validator;
import com.oracle.oci.intellij.ui.appstack.models.VariableGroup;
import com.oracle.oci.intellij.ui.common.CompartmentSelection;
import com.oracle.oci.intellij.ui.common.UIUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.ItemEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyDescriptor;
import java.beans.PropertyVetoException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CustomWizardStep extends WizardStep implements PropertyChangeListener {
    JBScrollPane mainScrollPane;
    JPanel mainPanel;
    VariableGroup variableGroup;
    boolean dirty = true ;
    List<VarPanel> varPanels ;
    Controller controller = Controller.getInstance();
    static private List<Stack> stackList ;


    public CustomWizardStep(VariableGroup varGroup, PropertyDescriptor[] propertyDescriptors, LinkedHashMap<String, PropertyDescriptor> descriptorsState, List<VariableGroup> varGroups) {
        mainPanel = new JPanel();
        mainScrollPane = new JBScrollPane(mainPanel);
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        varPanels = new ArrayList<>();
        varGroup.addPropertyChangeListener(this);
        varGroup.addVetoableChangeListener(new Validator());
        this.variableGroup = varGroup;


        controller.setDescriptorsState(descriptorsState) ;

        String className = varGroup.getClass().getSimpleName().replaceAll("_"," ");
        Border emptyBorder = BorderFactory.createEmptyBorder();
        TitledBorder titledBorder = BorderFactory.createTitledBorder(emptyBorder, className);

        Font currentFont = titledBorder.getTitleFont();
        if (currentFont == null) {
            currentFont = UIManager.getFont("TitledBorder.font");
        }
        titledBorder.setTitleFont(currentFont.deriveFont(Font.BOLD));
        mainPanel.setBorder(titledBorder);


        for (PropertyDescriptor pd : propertyDescriptors) {
            if (pd.getName().equals("class") ) {
                continue;
            }
            try {
                VarPanel varPanel ;
                if (pd.getName().equals("descriptionText"))
                    varPanel = new TextAreaVarPanel(pd,variableGroup);
               else
                    varPanel =new VarPanel(pd,variableGroup);
               varPanels.add(varPanel) ;
               controller.addVariablePanel( varPanel);
               mainPanel.add(varPanel)  ;
            } catch (InvocationTargetException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

    }

    public List<VarPanel> getVarPanels() {
        return varPanels;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    @Override
    public JComponent prepare(WizardNavigationState state) {
        return mainScrollPane;
    }

    @Override
    public WizardStep onNext(WizardModel model) {
        boolean isValidated = controller.doValidate(this);

        setDirty(!isValidated);



        CustomWizardModel appStackWizardModel = (CustomWizardModel) model;
        AppStackParametersWizardDialog.isProgramaticChange = true;
        appStackWizardModel.getGroupMenuList().setSelectedIndex(appStackWizardModel.getGroupMenuList().getSelectedIndex()+1);
        AppStackParametersWizardDialog.isProgramaticChange = false;
        return super.onNext(model);
    }



    @Override
    public boolean onFinish() {
        boolean isValidated = controller.doValidate(this);
        setDirty(!isValidated);

        return super.onFinish();
    }

    @Override
    public WizardStep onPrevious(WizardModel model) {
        boolean isValidated = controller.doValidate(this);
        setDirty(!isValidated);

        CustomWizardModel appStackWizardModel = (CustomWizardModel) model;
        AppStackParametersWizardDialog.isProgramaticChange = true;
        appStackWizardModel.getGroupMenuList().setSelectedIndex(appStackWizardModel.getGroupMenuList().getSelectedIndex()-1);
        AppStackParametersWizardDialog.isProgramaticChange = false;
        return super.onPrevious(model);
    }



    @Override
    public void propertyChange(PropertyChangeEvent evt) {

        // execute the updateDependency ...
        controller.updateDependencies(evt.getPropertyName(),variableGroup);
        // execute update-visibility ..... for the pd that changed
        controller.updateVisibility(evt.getPropertyName(),variableGroup);

    }

    public class VarPanel extends JPanel {
        JLabel label ;
        JComponent mainComponent ;
        JComponent inputComponent;
        JLabel errorLabel;
        PropertyDescriptor pd;
        VariableGroup variableGroup;
        protected ResourceBundle resBundle;




        VarPanel(PropertyDescriptor pd, VariableGroup variableGroup) throws InvocationTargetException, IllegalAccessException {
                this(pd,variableGroup,true);

        }

        VarPanel(PropertyDescriptor pd, VariableGroup variableGroup,boolean createVarPanel) throws InvocationTargetException, IllegalAccessException {
            this.pd = pd;
            this.variableGroup = variableGroup;
            resBundle = ResourceBundle.getBundle("appStackWizard", Locale.ROOT);

            if (createVarPanel){
                createVarPanel(pd,variableGroup);
            }
        }
        private void createVarPanel( PropertyDescriptor pd,VariableGroup variableGroup) throws InvocationTargetException, IllegalAccessException {
            setLayout(new BorderLayout());
            String varTitle = "";
            if (pd.getValue("required").equals(false)){
                varTitle+=" (Optional)";
            }

            label = new JLabel("<html><body style='width: 175px'>"+pd.getDisplayName()+" <i>"+varTitle+"</i></body></html>");
            if (!pd.getValue("type").equals("textArea"))
                setPreferredSize(new JBDimension(760, 40));
            else
                setPreferredSize(new JBDimension(760,100));
            label.setPreferredSize(new JBDimension(250,45));
            setMaximumSize(getPreferredSize());

            label.setToolTipText( pd.getShortDescription());


            errorLabel = new JLabel();
            errorLabel.setForeground(JBColor.RED);
            errorLabel.setBorder(BorderFactory.createEmptyBorder(0,5,0,0));





             mainComponent = createVarComponent(pd,variableGroup,errorLabel);


            boolean  isVisible = controller.isVisible((String) pd.getValue("visible"));
            this.setVisible(isVisible);


            add(label, BorderLayout.WEST);
            add(mainComponent,BorderLayout.CENTER);
            add(errorLabel,BorderLayout.EAST);


            setBorder(BorderFactory.createEmptyBorder(0,8,8,0));

        }



        private JComponent createVarComponent(PropertyDescriptor pd,VariableGroup varGroup,JLabel errorLabel) throws InvocationTargetException, IllegalAccessException {
            Class<?> propertyType = pd.getPropertyType();
            JComponent component ;

            if (propertyType.getName().equals("boolean")) {

                JCheckBox checkBox = new JCheckBox();
                component = checkBox;
                boolean defaultValue = (boolean)(pd.getValue("default")!= null?pd.getValue("default"):false );
                controller.setValue(defaultValue,varGroup,pd);
                checkBox.setSelected(defaultValue);

                checkBox.addActionListener(e -> {
                        controller.setValue(checkBox.isSelected(),varGroup,pd);
                });


                // add this to the condition || ((String)pd.getValue("type")).startsWith("oci")
            } else if (propertyType.isEnum() || ((String)pd.getValue("type")).startsWith("oci")  ) {

                // if it's an compartment object


                if (pd.getValue("type").equals("oci:identity:compartment:id")){
                    JPanel compartmentPanel = new JPanel();
                    JButton selectCompartmentBtn  = new JButton("Select");
                    JTextField compartmentName = new JTextField("");
                    compartmentName.setPreferredSize(new JBDimension(409,30));
                    compartmentName.setEnabled(false);
                    compartmentPanel.add(compartmentName);
                    compartmentPanel.add(selectCompartmentBtn);
                    Compartment selectedCompartment = (Compartment) controller.getValue(varGroup,pd);

                    compartmentName.setText(selectedCompartment.getName());

                    ExecutorService executorService = Executors.newSingleThreadExecutor();

                    selectCompartmentBtn.addActionListener(e->{
                        final CompartmentSelection compartmentSelection1 = CompartmentSelection.newInstance();


                        if (compartmentSelection1.showAndGet()){
                            final Compartment selected = compartmentSelection1.getSelectedCompartment();
                            compartmentName.setText(selected.getName());
                            executorService.submit(() -> {
                                controller.setValue(selected,varGroup,pd);
                                return null;
                            });
                        }
                    });
                    inputComponent = compartmentName;
                    return compartmentPanel;
                }else {

                    ComboBox comboBox = new ComboBox();
                    AtomicReference<List<String>> enumValues = new AtomicReference<>((List<String>) pd.getValue("enum"));
                    if (enumValues.get() != null) {
                        for (String enumValue : enumValues.get()) {
                            Class<?> type = pd.getPropertyType();
                            String normalizedItem =enumValue.trim().replaceAll("[. ]","_");

                            Enum<?> enumValue1 = Enum.valueOf((Class<Enum>) type, normalizedItem);
                            comboBox.addItem(enumValue1);
                        }
                        if (pd.getValue("default") != null) {
                            controller.setValue(pd.getValue("default"),varGroup,pd);
                            comboBox.setSelectedItem(pd.getValue("default"));
                        }else{
                            controller.setValue(comboBox.getSelectedItem(),varGroup,pd);
                        }

                    } else {
                        //todo  suggest values from account of user   in a combobox depending on  type
                        /* example
                         * oci:identity:compartment:id --> compartments of the user
                         * oci:core:vcn:id --> existed vcn s ...
                         *
                         */
                        // we need to set a custom renderer
                        comboBox.setModel(new DefaultComboBoxModel<>(new String[] {"Loading..."}));
                        controller.loadComboBoxValues(pd,varGroup,comboBox);
                        System.out.println("----------"+pd.getName()+"----------------");


                        comboBox.setRenderer(new DefaultListCellRenderer(){

                            //todo enhance this later
                            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                                if (value instanceof AutonomousDatabaseSummary) {
                                    AutonomousDatabaseSummary adb = (AutonomousDatabaseSummary) value;
                                    setText(adb.getDisplayName()+" ("+getId(adb.getId())+")"); // Set the display name of the instance
                                } else if (value instanceof VaultSummary) {
                                    VaultSummary adb = (VaultSummary) value;
                                    setText(adb.getDisplayName()+" ("+getId(adb.getId())+")"); // Set the display name of the instance
                                }else if(value instanceof RepositorySummary){
                                    RepositorySummary repositorySummary = (RepositorySummary)value;
                                    setText(repositorySummary.getName()+" ("+getId(repositorySummary.getId())+")");
                                }else if (value instanceof KeySummary) {
                                    KeySummary adb = (KeySummary) value;
                                    setText(adb.getDisplayName()+" ("+getId(adb.getId())+")"); // Set the display name of the instance
                                }else if (value instanceof AvailabilityDomain) {
                                    AvailabilityDomain adb = (AvailabilityDomain) value;
                                    setText(adb.getName()+" ("+getId(adb.getId())+")"); // Set the display name of the instance
                                }else if (value instanceof Subnet) {
                                    Subnet adb = (Subnet) value;
                                    setText(adb.getDisplayName()+" ("+getId(adb.getId())+")"); // Set the display name of the instance
                                }else if (value instanceof Vcn) {
                                    Vcn adb = (Vcn) value;
                                    setText(adb.getDisplayName()+" ("+getId(adb.getId())+")"); // Set the display name of the instance
                                }else if (value instanceof Compartment) {
                                    Compartment adb = (Compartment) value;
                                    setText(adb.getName()+" ("+getId(adb.getId())+")"); // Set the display name of the instance
                                }else if (value instanceof ZoneSummary) {
                                    ZoneSummary zone = (ZoneSummary) value;
                                    setText(zone.getName()+" ("+getId(zone.getId())+")"); // Set the display name of the instance
                                }else if(value instanceof RepositoryBranchSummary){
                                    RepositoryBranchSummary repositoryBranchSummary = (RepositoryBranchSummary) value;
                                    setText(repositoryBranchSummary.getRefName());
                                }else if(value == null){
                                    setText("No items");
                                }
                                return this;
                            }
                        });


                    }

                    comboBox.addItemListener(e -> {
                        if (e.getStateChange() == ItemEvent.SELECTED) {
                            controller.setValue(comboBox.getSelectedItem(),varGroup,pd);
                        }

                    });



                    component = comboBox;
                }
            } else if (propertyType.getName().equals("int")) {
                SpinnerNumberModel spinnerModel = new SpinnerNumberModel(0, 0, Integer.MAX_VALUE, 1);
                JSpinner spinner = new JSpinner(spinnerModel);

                JComponent editorComponent = spinner.getEditor();

                if (editorComponent instanceof JSpinner.DefaultEditor) {
                    JTextField textField = ((JSpinner.DefaultEditor) editorComponent).getTextField();
                    textField.addFocusListener(new FocusAdapter() {
                        @Override
                        public void focusLost(FocusEvent e) {
                            controller.setValue(spinner.getValue(),varGroup,pd);
                           focusValidation(spinner.getValue());
                        }
                    });
                }


                Object value = pd.getValue("default");
                if (value != null){
                    if (value instanceof String) {
                        if (((String)value).isEmpty()){
                            value = 0;
                        } else {
                            value = Integer.parseInt((String) value);
                        }
                    }
                    controller.setValue(value,varGroup,pd);
                    spinner.setValue(value);

                }

                spinner.addChangeListener(e->{
                    controller.setValue(spinner.getValue(),varGroup,pd);
                });


                component = spinner;
            } else {

                JTextComponent textField = getjTextField(pd, varGroup);

                if (pd.getValue("default") != null){
                    textField.setText(pd.getValue("default").toString());
                    controller.setValue(pd.getValue("default").toString(),varGroup,pd);
                }

                if (pd.getName().equals("current_user_token")){
                    JPanel userTokenPanel = new JPanel(new BorderLayout());

                    JButton listTokenButton = getListTokenButton();

                    inputComponent = textField ;
                    textField.setPreferredSize(new JBDimension(400,-1));
                    userTokenPanel.add(textField,BorderLayout.WEST);
                    userTokenPanel.add(listTokenButton,BorderLayout.CENTER);
                    return userTokenPanel ;
                }
                component = textField;
            }
            component.setPreferredSize(new JBDimension(200,100));

            inputComponent = component;
            return component;
        }

        @NotNull
        private JButton getListTokenButton() {
            JButton listTokenButton = new JButton("List");

            listTokenButton.addActionListener((event)->{
//                        OracleCloudAccount.IdentityClientProxy identityClientProxy = OracleCloudAccount.getInstance().getIdentityClient();
//                        List<AuthToken> tokens = identityClientProxy.getAuthTokenList();
                listTokenButton.setEnabled(false);
                AuthenticationTokenDialog authenticationTokenDialog = new AuthenticationTokenDialog();
                authenticationTokenDialog.show();
                listTokenButton.setEnabled(true);
            });
            return listTokenButton;
        }

        String getId(String ocid){
            int start = ocid.length() - 9;
            return ocid.substring(start);
        }

        private void errorCheck(PropertyDescriptor pd, JLabel errorLabel, JSpinner spinner) {
            if ( pd.getValue("required") != null && (boolean) pd.getValue("required")   ) {
                String errorMsg ="This field is required";

                if ((int) spinner.getValue() == 0){
                    errorMsg = "this field can't be 0";
                }else {
                    spinner.setBorder(UIManager.getBorder("TextField.border")); // Reset to default border
                    errorLabel.setText("");
                    return;
                }

                spinner.setBorder(BorderFactory.createLineBorder(JBColor.RED));
                errorLabel.setText(errorMsg);
            }


        }


        public JLabel getLabel() {
            return label;
        }

        public void setLabel(JLabel label) {
            this.label = label;
        }

        public JComponent getMainComponent() {
            return mainComponent;
        }

        public JComponent getInputComponent() {
            return inputComponent;
        }

        public void setMainComponent(JComponent mainComponent) {
            this.mainComponent = mainComponent;
        }

        public JLabel getErrorLabel() {
            return errorLabel;
        }

        public void setErrorLabel(JLabel errorLabel) {
            this.errorLabel = errorLabel;
        }

        public PropertyDescriptor getPd() {
            return pd;
        }

        public void setPd(PropertyDescriptor pd) {
            this.pd = pd;
        }

        public VariableGroup getVariableGroup() {
            return variableGroup;
        }

        public void setVariableGroup(VariableGroup variableGroup) {
            this.variableGroup = variableGroup;
        }

        private JTextComponent getjTextField(PropertyDescriptor pd, VariableGroup varGroup) {
            JTextComponent textField  ;
            Font commonFont = new JBTextField().getFont();
            if (pd.getValue("type").equals("password")){
                textField = new JBPasswordField();
            } else if (pd.getValue("type").equals("textArea")) {
                JTextArea textArea = new JTextArea(4,20);
                Font newFont = new Font(commonFont.getName(),commonFont.getStyle(),13);
                textArea.setFont(newFont);
                Insets currentInsets = textArea.getMargin();
                Insets newInsets = new Insets(10, 10, currentInsets.bottom, currentInsets.right);
                textArea.setMargin(newInsets);
//                textArea.setBorder(UIManager.getBorder("TextField.border")); // Reset to default border
                textField = textArea;
            } else {
                textField = new JBTextField();
            }
            textField.addFocusListener(new FocusAdapter() {

                @Override
                public void focusGained(FocusEvent e) {
                    textField.selectAll();
                    super.focusGained(e);
                }

                @Override
                public void focusLost(FocusEvent e) {
                   controller.setValue(textField.getText(),varGroup,pd);
                   focusValidation(textField.getText());
                }
               }

            );


            textField.getDocument().addDocumentListener(new DocumentListener() {
                @Override
                public void insertUpdate(javax.swing.event.DocumentEvent e) {
                    documentChanged();
                }

                @Override
                public void removeUpdate(javax.swing.event.DocumentEvent e) {
                    documentChanged();
                }

                @Override
                public void changedUpdate(javax.swing.event.DocumentEvent e) {documentChanged();}

                private void documentChanged() {
                    // Handle text field changes here
                        controller.setValue(textField.getText(),varGroup,pd);
                }

            });
            return textField;
        }

        void focusValidation(Object value){
            try {
                Validator.doValidate(pd,value,null);
                controller.handleValidated(pd);
            } catch (PropertyVetoException ex) {
                controller.handleError(pd,ex.getMessage());
            }
        }
    }

    public class TextAreaVarPanel extends VarPanel {
        JBTextArea appStackDescription;

        TextAreaVarPanel(PropertyDescriptor pd, VariableGroup variableGroup) throws InvocationTargetException, IllegalAccessException {
            super(pd, variableGroup, false);
            setLayout(new BorderLayout());
            appStackDescription = new JBTextArea();

            Insets currentInsets = appStackDescription.getMargin();
            Insets newInsets = new Insets(8, 8, currentInsets.bottom, currentInsets.right);
            appStackDescription.setMargin(newInsets);
            appStackDescription.setEditable(false);
            appStackDescription.setWrapStyleWord(true);
            appStackDescription.setLineWrap(true);
            appStackDescription.setColumns(30);

            ResourceBundle messages = super.resBundle;
            appStackDescription.setText(messages.getString("app"));
            appStackDescription.setFont(new Font("Arial", Font.PLAIN, 14));

            JBScrollPane jbScrollPane = new JBScrollPane(appStackDescription);
            JPanel mainPanel = new JPanel();
            mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
            mainPanel.add(jbScrollPane);

            JPanel linkPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            linkPanel.add(new JBLabel("For more information and product documentation please visit the "));

            ActionLink link = new ActionLink("App Stack Documentation", (ActionListener) e -> UIUtil.browseLink("https://github.com/oracle-quickstart/appstack"));
            linkPanel.add(link);

            mainPanel.setPreferredSize(new JBDimension(800, 0));
            mainPanel.add(linkPanel);
            add(mainPanel, BorderLayout.WEST);
        }
    }
}






