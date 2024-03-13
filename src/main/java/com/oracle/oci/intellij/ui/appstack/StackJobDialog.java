package com.oracle.oci.intellij.ui.appstack;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import com.intellij.ui.components.JBTextArea;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBDimension;
import com.oracle.bmc.resourcemanager.model.Job;
import com.oracle.oci.intellij.ui.common.MyBackgroundTask;
import com.oracle.oci.intellij.ui.common.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.intellij.openapi.ui.DialogWrapper;
import com.oracle.bmc.resourcemanager.model.JobSummary;
import com.oracle.oci.intellij.account.OracleCloudAccount;
import com.oracle.oci.intellij.ui.appstack.command.TerraformLogger;

public class StackJobDialog extends DialogWrapper {

  private final List<JobSummary> jobs;      
  private TerraformLogger logger;
  private JPanel mainPanel;
  private JBTable jobsTable;
  private JBTextArea textArea;
  private JComboBox operationTypeCombobox;
  private JComboBox statusComboBox;
  private JButton searchButton;
  private JButton resetButton;

  protected StackJobDialog(List<JobSummary> jobs) {
    super(true);
    this.jobs = new ArrayList<>(jobs);
    init();
//    setTitle("Stack Jobs");
    setOKButtonText("Ok");
    setSize(1000,900);
//    filterPanel.setMaximumSize(new JBDimension(0,30));
   searchButton.addActionListener(e -> {
     filterJobs();
   });
    resetButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        statusComboBox.setSelectedItem("ALL");
        operationTypeCombobox.setSelectedItem("ALL");
        filterJobs();
      }
    });


    jobsTable.getColumn("Status").setCellRenderer(new DefaultTableCellRenderer() {
      @Override
      public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        if (column == 2) {
          super.getTableCellRendererComponent(table,value,isSelected,hasFocus,row,column);
          final Job.LifecycleState  state = (Job.LifecycleState) value;
          this.setText(state.getValue());
          this.setIcon(AppStackDashboard.getImageStatus(state));
          return this;
        }
        return (Component) value;
      }
    });
  }

  private void filterJobs() {
    String operationType = (String) operationTypeCombobox.getSelectedItem().toString();
    String status = (String) statusComboBox.getSelectedItem();
    List<JobSummary> newJobs = jobs.stream().filter(jobSummary -> {
       boolean matchesOperationType = operationType.equals("ALL") || jobSummary.getOperation().getValue().equalsIgnoreCase(operationType);
       boolean matchesStatus = status.equals("ALL") || jobSummary.getLifecycleState().getValue().equalsIgnoreCase(status);

       return matchesStatus && matchesOperationType ;
    }).collect(Collectors.toList());

    UIUtil.invokeLater(()->{
      DefaultTableModel jobsModel = (DefaultTableModel) jobsTable.getModel();
      jobsModel.setRowCount(0);
      List<Object> row = new ArrayList<>();
      newJobs.forEach(j->{
        row.add(j.getDisplayName());
        row.add(j.getOperation());
        row.add(j.getLifecycleState());
        row.add(j.getTimeCreated());
        jobsModel.addRow(row.toArray());
        row.clear();
      });
    });
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
      String status = j.getLifecycleState().getValue();
      if (Job.LifecycleState.InProgress.getValue().equalsIgnoreCase(status) ||
              Job.LifecycleState.Canceling.getValue().equalsIgnoreCase(status) ||
              Job.LifecycleState.Accepted.getValue().equalsIgnoreCase(status)
      ){
        // update the job's status
        boolean b =updateJobStateInBackground(j.getId());
      }
      row.add(j.getDisplayName());
      row.add(j.getOperation());
      row.add(j.getLifecycleState());
      row.add(j.getTimeCreated());
      jobsModel.addRow(row.toArray());
      row.clear();
    });

//    JBTable jobsTable = new JBTable();
    jobsTable.setModel(jobsModel);
//    JScrollPane tableScrollPane = new JScrollPane(jobsTable);
//    tableScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
//    tableScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

//    centerPanel.add(tableScrollPane);
//    JPanel leftPanel = new JPanel(new BorderLayout());

//    leftPanel.add(tableScrollPane, BorderLayout.NORTH);
//    leftPanel.setBorder(JBUI.Borders.customLine(JBColor.black));
//    centerPanel.add(leftPanel);
//    panel.add(Box.createRigidArea(new Dimension(0, 10))); // Add 10px vertical spacing
//    centerPanel.add(Box.createRigidArea(new JBDimension(0,10)));
//    centerPanel.add(new Empty)
//     textArea = new JBTextArea("Select a job to see it's logs");
    textArea.setText("Select a job to see its logs ");
    textArea.setMargin(new Insets(3,9,3,3));
    textArea.setLineWrap(true);
    textArea.setEditable(false);
    textArea.setVisible(true);
    textArea.setColumns(80);
    textArea.setRows(30);

//    JScrollPane scroll = new JBScrollPane(textArea);
//    scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
//    scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);


//    centerPanel.add(scroll);
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

    return mainPanel;
  }

  private boolean updateJobStateInBackground(String jobId) {
    UIUtil.schedule(()->{
      while (true){
        DefaultTableModel model = (DefaultTableModel ) jobsTable.getModel();
        Job job= MyBackgroundTask.getJob(jobId);
        Job.LifecycleState state = job.getLifecycleState();
        SwingUtilities.invokeLater(()->{
          try {
          // row 0 the  last job
          model.setValueAt(state,0,2);
          model.fireTableCellUpdated(0,2);
        }catch (ArrayIndexOutOfBoundsException ex){
          System.out.println(ex.getMessage());
        }
        });
        if ("SUCCEEDED".equals(state)) {
          return ;
        } else if ("FAILED".equals(state)) {
          return ;
        }

          try {
              Thread.sleep(5000); // Sleep for 5 seconds
          } catch (InterruptedException e) {
              throw new RuntimeException(e);
          }


      }
    });
    return false;
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
      this.target.setText("Fetching logs...... Please Wait");
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
              fullBuffer.append(buffer);
              if (isFinished)
                target.setText(fullBuffer.toString());
              else
                target.setText(fullBuffer+"\nFetching logs...  Please Wait...");
              buffer.setLength(0);
            }
          });
        } catch (InvocationTargetException | InterruptedException e) {
          throw new IOException(e);
        }
      }else {
        if (fullBuffer.toString().isEmpty()){
          UIUtil.invokeLater(()->{
            target.setText(fullBuffer+"\nFetching logs...  Please Wait...");
          });
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
    }

    @Override
    public void close() throws IOException {
      this.buffer = null;
      this.target = null;
      this.fullBuffer = null;
    }
  }
}