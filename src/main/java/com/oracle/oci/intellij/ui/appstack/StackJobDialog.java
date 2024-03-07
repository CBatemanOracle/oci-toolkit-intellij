package com.oracle.oci.intellij.ui.appstack;

import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;

import com.intellij.openapi.ui.DescriptionLabel;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBTextArea;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBDimension;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.intellij.openapi.ui.DialogWrapper;
import com.oracle.bmc.resourcemanager.model.JobSummary;
import com.oracle.oci.intellij.account.OracleCloudAccount;
import com.oracle.oci.intellij.ui.appstack.command.TerraformLogger;

public class StackJobDialog extends DialogWrapper {

  private final List<JobSummary> jobs;      
  private TerraformLogger logger;


  protected StackJobDialog(List<JobSummary> jobs) {
    super(true);
    this.jobs = new ArrayList<>(jobs);
    init();
    setTitle("Stack Job");
    setOKButtonText("Ok");
  }

  @Override
  protected void dispose() {
    super.dispose();
    if (logger != null) {
      logger.close();
      logger = null;
    }
  }

  @Override
  protected @Nullable JComponent createCenterPanel() {
    JPanel centerPanel = new JPanel();
    centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));

    DefaultTableModel jobsModel = new DefaultTableModel();
    jobsModel.addColumn("Name");
    jobsModel.addColumn("Operation");
    jobsModel.addColumn("Status");
    jobsModel.addColumn("Time Created");
    List<Object> row = new ArrayList<>();
    this.jobs.forEach(j -> {
      row.add(j.getDisplayName());
      row.add(j.getOperation());
      row.add(j.getLifecycleState());
      row.add(j.getTimeCreated());
      jobsModel.addRow(row.toArray());
      row.clear();
    });

    JBTable jobsTable = new JBTable();
    jobsTable.setModel(jobsModel);
    JPanel leftPanel = new JPanel(new BorderLayout());
    leftPanel.add(jobsTable, BorderLayout.NORTH);
    leftPanel.setBorder(JBUI.Borders.customLine(JBColor.black));
    centerPanel.add(leftPanel);
//    panel.add(Box.createRigidArea(new Dimension(0, 10))); // Add 10px vertical spacing
    centerPanel.add(Box.createRigidArea(new JBDimension(0,10)));
//    centerPanel.add(new Empty)
    JBTextArea textArea = new JBTextArea();
    // textArea.setText("Hello!");
    textArea.setLineWrap(true);
    textArea.setEditable(false);
    textArea.setVisible(true);
    textArea.setColumns(80);
    textArea.setRows(30);

    JScrollPane scroll = new JScrollPane(textArea);
    scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
    scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);


    JBPanel rightPanel = new JBPanel();

    centerPanel.add(scroll);
    // centerPanel.add(textArea);

    jobsTable.addMouseListener(new MouseAdapter() {


      @Override
      public void mouseClicked(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) {
          if (jobsTable.getSelectedRowCount() == 1) {
            int selectedRow = jobsTable.getSelectedRow();
            JobSummary jobSummary = jobs.get(selectedRow);
            String id = jobSummary.getId();
            textArea.setText(null);
            if (logger != null) {
              logger.close();
            }
            logger =
              new TerraformLogger(new JTextAreaWriter(textArea),
                                  OracleCloudAccount.getInstance()
                                                    .getResourceManagerClientProxy(),
                                  id);
            logger.start();
          }
        }
      }
    });

    return centerPanel;
  }
  @Override
  protected Action @NotNull [] createActions() {
//    if (isShowStackVariables){
      getCancelAction().putValue("Name","Close");
      return new Action[]{getCancelAction()};
//    }
//    return super.createActions();
  }

  public static class JTextAreaWriter extends Writer {

    private boolean isFinished ;
    private JBTextArea target;
    private StringBuilder buffer;
    private StringBuilder fullBuffer ;

    public JTextAreaWriter(JBTextArea target) {
      super();
      this.target = target;
      this.target.setText("     Loading... Please Wait");
      this.buffer = new StringBuilder();
      this.fullBuffer = new StringBuilder();
    }
    public void setFinished(boolean finished) {
      isFinished = finished;
    }

    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
      this.buffer.append(cbuf, off, len);
    }

    @Override
    public void flush() throws IOException {
      if (this.buffer.length() > 0) {
        try {
          // wait to ensure ordering.
          SwingUtilities.invokeAndWait(() -> {
            if (buffer.length() > 0) {
//              StringBuilder fullBuffer = new StringBuilder();
              fullBuffer.append(buffer);
              if (isFinished)
                target.setText(fullBuffer.toString());
              else
                target.setText(fullBuffer+"\n    Loading ... Please Wait");
              buffer.setLength(0);
            }
          });
        } catch (InvocationTargetException | InterruptedException e) {
          throw new IOException(e);
        }
      }else {
          try {
              SwingUtilities.invokeAndWait(() -> {
                target.setText(fullBuffer.toString());

              });
          } catch (InterruptedException e) {
              throw new RuntimeException(e);
          } catch (InvocationTargetException e) {
              throw new RuntimeException(e);
          }
      }
    }

    @Override
    public void close() throws IOException {
      this.buffer = null;
      this.target = null;
      this.fullBuffer = null;
    }
  }
}