package com.oracle.oci.intellij.ui.appstack.actions;

import com.intellij.ui.components.*;
import com.intellij.ui.wizard.WizardModel;
import com.intellij.ui.wizard.WizardNavigationState;
import com.intellij.ui.wizard.WizardStep;
import com.intellij.util.ui.JBDimension;
import com.oracle.oci.intellij.ui.common.UIUtil;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Locale;
import java.util.ResourceBundle;

public class IntroductoryWizardStep extends AbstractWizardStep  {
    JBScrollPane mainScrollPane;
    ResourceBundle resBundle;
    JPanel mainPanel;
    IntroductoryWizardStep(){
        initComponents();
    }

    private void initComponents() {
        mainPanel = new JPanel();
        resBundle = ResourceBundle.getBundle("appStackWizard", Locale.ROOT);
        Border emptyBorder = BorderFactory.createEmptyBorder();
        TitledBorder titledBorder = BorderFactory.createTitledBorder(emptyBorder, "Introduction");
        Font currentFont = titledBorder.getTitleFont();
        if (currentFont == null) {
            currentFont = UIManager.getFont("TitledBorder.font");
        }
        titledBorder.setTitleFont(currentFont.deriveFont(Font.BOLD));
        mainPanel.setBorder(titledBorder);

        JBTextArea appStackDescription;

        mainScrollPane = new JBScrollPane(mainPanel);

        mainPanel.setLayout(new BorderLayout());
        appStackDescription = new JBTextArea();

        Insets currentInsets = appStackDescription.getMargin();
        Insets newInsets = new Insets(8, 8, currentInsets.bottom, currentInsets.right);
        appStackDescription.setMargin(newInsets);
        appStackDescription.setEditable(false);
        appStackDescription.setWrapStyleWord(true);
        appStackDescription.setLineWrap(true);
        appStackDescription.setColumns(30);

        ResourceBundle messages = resBundle;
        appStackDescription.setText(messages.getString("app"));
        appStackDescription.setFont(new Font("Arial", Font.PLAIN, 14));

        JBScrollPane jbScrollPane = new JBScrollPane(appStackDescription);
        jbScrollPane.setPreferredSize(new JBDimension(700,5));
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(jbScrollPane);

        JPanel linkPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        linkPanel.add(new JBLabel("All the documention is availbale on the github project "));

        ActionLink link = new ActionLink("App Stack Documentation", (ActionListener) e -> UIUtil.browseLink("https://github.com/oracle-quickstart/appstack"));
        linkPanel.add(link);

        panel.add(linkPanel);

        JPanel dontShowPanel = new JBPanel<>(new FlowLayout(FlowLayout.LEFT));
        JBCheckBox dontShowCheckBox = new JBCheckBox();
        dontShowCheckBox.addActionListener(e->{
            boolean introductionDontShowAgain = dontShowCheckBox.isSelected();

            controller.setState(introductionDontShowAgain);
        });

        JBLabel dontShowLabel =new JBLabel("Don't show again" );
        dontShowPanel.add(dontShowCheckBox);
        dontShowPanel.add(dontShowLabel);
        panel.add(dontShowPanel);
        dontShowLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                dontShowCheckBox.setSelected(!dontShowCheckBox.isSelected());
                super.mouseClicked(e);
            }
        });
        panel.setPreferredSize(new JBDimension(800, 0));

        JBPanel emtpy = new JBPanel<>();
        emtpy.setPreferredSize(new JBDimension(800,30));

        panel.add(emtpy);
        linkPanel.setMaximumSize(new JBDimension(800,20));
        dontShowPanel.setMaximumSize(new JBDimension(800,20));
        mainPanel.add(panel, BorderLayout.WEST);
    }

    @Override
    public WizardStep onNext(WizardModel model) {
        setDirty(false);
        return super.onNext(model);
    }

    @Override
    public WizardStep onPrevious(WizardModel model) {
        setDirty(false);
        return super.onPrevious(model);
    }

    @Override
    public JComponent prepare(WizardNavigationState wizardNavigationState) {
        return mainScrollPane;
    }


}
