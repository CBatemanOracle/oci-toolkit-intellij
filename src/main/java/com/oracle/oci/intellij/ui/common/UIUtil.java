/*
  Copyright (c) 2021, Oracle and/or its affiliates.
  Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */
package com.oracle.oci.intellij.ui.common;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.ui.JBColor;
import com.oracle.oci.intellij.account.SystemPreferences;
import com.oracle.oci.intellij.util.fills.NotificationGroupShim;
import com.oracle.oci.intellij.util.fills.Shim;
import com.oracle.oci.intellij.util.fills.ShimMethod;
import io.github.resilience4j.core.lang.NonNull;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

@Shim(forType = com.intellij.util.ui.UIUtil.class, dedicated = false)
public class UIUtil {
  private static Project currentProject;

  private static @NlsSafe final String NOTIFICATION_GROUP_ID =
    "Oracle Cloud Infrastructure";

  public static void setCurrentProject(@NotNull Project project) {
    currentProject = project;
  }

  public static void showInfoInStatusBar(@NotNull final String info) {
    if (currentProject != null) {
      WindowManager.getInstance().getStatusBar(currentProject).setInfo(info);
    }
  }

  /**
   * @return true if theme is dark.  This shims the deprecated UIUtil.isUnderDarcula()
   * call from JB's UIUtil, following the deprecation instruction to use JBColor.isBright().
   */
  @ShimMethod(methodName = "UIUtil.isUnderDarcula()")
  public static boolean isUnderDarcula() {
    return !JBColor.isBright();
  }

  public static boolean isDarkMode() {
    return isUnderDarcula();
  }

  public static void fireNotification(NotificationType notificationType,
                                      @NotNull final String msg) {
    fireNotification(notificationType, msg, null);
  }

  public static void fireNotification(NotificationType notificationType,
                                      @NotNull final String msg,
                                      String eventName) {
    invokeLater(() -> {
      NotificationGroup notificationGroup =
        NotificationGroupManager.getInstance().getNotificationGroup(NOTIFICATION_GROUP_ID);
      NotificationGroupShim groupShim = new NotificationGroupShim(notificationGroup);
      Notification notification =
        groupShim.createNotification(NOTIFICATION_GROUP_ID, "", msg, notificationType);
      notification.notify(currentProject);

      if (eventName != null) {
        SystemPreferences.fireADBInstanceUpdateEvent(eventName);
      }
    });
  }

  public static void warn(@NotNull final String msg) {
    fireNotification(NotificationType.WARNING, msg, "");
  }

  public static void executeAndUpdateUIAsync(@NotNull Runnable action,
                                             @Nullable Runnable update) {
    executeAndUpdateUIAsync(action, update, null);
  }

  public static void executeAndUpdateUIAsync(@NotNull Runnable action,
                                             @Nullable Runnable update,
                                             @Nullable ModalityState state) {
    ApplicationManager.getApplication().executeOnPooledThread(() -> {
      action.run();
      if (update != null) {
        invokeLater(update, state);
      }
    });
  }
  
  public static void invokeLater(Runnable runnable, ModalityState modalityState) {
    if (modalityState == null) {
      invokeLater(runnable);
    }
    else {
      ApplicationManager.getApplication().invokeLater(runnable, modalityState);
    }
  }

  public static  void invokeAndWait(Runnable runnable,ModalityState modalityState){
    if (modalityState == null){
      ApplicationManager.getApplication().invokeAndWait(runnable);
    }else {
      ApplicationManager.getApplication().invokeAndWait(runnable,modalityState);
    }
  }
  public static void invokeLater(Runnable runnable) {
    ApplicationManager.getApplication().invokeLater(runnable);
  }
  public static void schedule(Runnable runnable){
    ApplicationManager.getApplication().executeOnPooledThread(() -> {
          runnable.run();
    });
  }

  public static IconButton createButtonIcon( String showIconPath) {
    IconButton iconButton = new IconButton(showIconPath);
    return iconButton;
  }

  public static class IconButton extends JButton {
    String iconPath;

    public IconButton(String iconPath) {
      super();
      this.iconPath = iconPath;
      initializeButton(iconPath);
    }

    private void initializeButton(String showIconPath) {
      this.setIcon(IconLoader.getIcon(showIconPath));

      this.setBackground(null);
      this.setBorder(null);
      this.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
      this.setOpaque(false);
      this.setFocusable(false);
      this.setContentAreaFilled(false);
      this.setPreferredSize(new Dimension(20, 20));
      this.setMargin(new Insets(0, 0, 0, 0));
      this.setHorizontalAlignment(SwingConstants.CENTER);
      this.setVerticalAlignment(SwingConstants.CENTER);
    }

    @Override
    public void updateUI() {
      super.updateUI();
      if (iconPath != null)
        initializeButton(iconPath);
    }
  }

  public static void createWebLink(JComponent component, String uri) {
    component.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    component.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        browseLink(uri);
      }
    });
  }

  public static void browseLink(String uri) {
    try {
      if (!Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
        return;
      }
      Desktop.getDesktop().browse(new URI(uri));
    } catch (URISyntaxException | IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  public static void showErrorDialog(Component parentComponent, String title, String message) {
    // Use Swing's JOptionPane to show a modal error dialog
    JOptionPane.showMessageDialog(parentComponent,message,title,JOptionPane.ERROR_MESSAGE);
  }

  public static SimpleDialogWrapper createDialog(String title,
                                                 boolean canBeParent) {
    SimpleDialogWrapper dialog = new SimpleDialogWrapper(canBeParent) {
      @Override
      protected @Nullable JComponent createCenterPanel() {
        JPanel panel = new JPanel();
        GridLayout layout = new GridLayout(1, 1);
        panel.setLayout(layout);

        panel.add(new JTextArea("Default SimpleDialogWrapper. Override createCenterPanel or provide fn"));
        return panel;
      }
    };
    dialog.setTitle(title);
    return dialog;
  }

  public static SimpleDialogWrapper createDialog(String title,
                                                 boolean canBeParent,
                                                 Function<SimpleDialogWrapper, JComponent> createCenterPane) {
    SimpleDialogWrapper dialog = new SimpleDialogWrapper(canBeParent);
    dialog.setTitle(title);
    return dialog;
  }

  public static class SimpleDialogWrapper extends DialogWrapper {

    private Function<SimpleDialogWrapper, JComponent> createCenterPanelFn;

    public SimpleDialogWrapper(boolean canBeParent) {
      this((Project) null, canBeParent);
    }

    protected SimpleDialogWrapper(@NotNull Component parent,
                                  boolean canBeParent) {
      super(parent, canBeParent);
      basicInit();
    }

    public SimpleDialogWrapper(@Nullable Project project, boolean canBeParent,
                               @NotNull IdeModalityType ideModalityType) {
      this(project, null, canBeParent, ideModalityType);
    }

    public SimpleDialogWrapper(@Nullable Project project, boolean canBeParent) {
      this(project, canBeParent, IdeModalityType.IDE);
    }

    public SimpleDialogWrapper(@Nullable Project project) {
      this(project, true);
    }

    public SimpleDialogWrapper(@Nullable Project project,
                               @Nullable Component parentComponent,
                               boolean canBeParent,
                               @NotNull IdeModalityType ideModalityType) {
      this(project, parentComponent, canBeParent, ideModalityType, true);
    }

    protected SimpleDialogWrapper(Project project, boolean canBeParent,
                                  boolean applicationModalIfPossible) {
      super(project, canBeParent, applicationModalIfPossible);
      basicInit();
    }

    protected SimpleDialogWrapper(@Nullable Project project,
                                  @Nullable Component parentComponent,
                                  boolean canBeParent,
                                  @NotNull IdeModalityType ideModalityType,
                                  boolean createSouth) {
      super(project, parentComponent, canBeParent, ideModalityType,
            createSouth);
    }

    protected void basicInit() {
      setOKButtonText("OK");
      init();
    }

    public void setCreatePanelFn(Function<SimpleDialogWrapper, JComponent> fn) {
      this.createCenterPanelFn = fn;
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
      if (this.createCenterPanelFn != null) {
        return this.createCenterPanelFn.apply(this);
      }
      return null;
    }
  }

  public static <MODEL> ModelHolder<MODEL> holdModel(MODEL m) {
    return new ModelHolder<MODEL>(m);
  }

  public static class ModelHolder<MODEL> implements Supplier<MODEL> {
    private final MODEL model;
    private Optional<Function<MODEL, String>> textProvider;

    public ModelHolder(@NotNull MODEL model) {
      this.model = model;
    }

    @Override
    public MODEL get() {
      return this.model;
    }

    @Override
    public int hashCode() {
      return this.model.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
      return this.model.equals(obj);
    }

    @Override
    public String toString() {
      return textProvider.orElse((m) -> {
        return m.toString();
      }).apply(this.model);
    }

    public ModelHolder<MODEL> setTextProvider(Function<MODEL, String> provider) {
      this.textProvider = Optional.of(provider);
      return this;
    }

    public static <T> Optional<T> getComboItem(@NonNull JComboBox<ModelHolder<T>> comboBox) {
      Object selectedItem = comboBox.getSelectedItem();
      @SuppressWarnings("unchecked")
      ModelHolder<T> selected = (ModelHolder<T>) selectedItem;
      return Optional.ofNullable(selected.get());
    }
  }

  public static class GridBagLayoutConstraintBuilder {
    private GridBagLayoutConstraintBuilder() {
      
    }
    private int gridx;
    private int gridy;
    private int gridwidth;
    private int gridheight;
    private int weightx;
    private int weighty;
    private int anchor;
    private int fill;
    private Insets insets;
    private int ipadx;
    private int ipady;

    public GridBagLayoutConstraintBuilder gridx(int gridx) {
      this.gridx = gridx;
      return this;
    }

    public GridBagLayoutConstraintBuilder gridy(int gridy) {
      this.gridy = gridy;
      return this;
    }

    public GridBagLayoutConstraintBuilder gridwidth(int width) {
      this.gridwidth = width;
      return this;
    }

    public GridBagLayoutConstraintBuilder gridHeight(int gridheight) {
      this.gridheight = gridheight;
      return this;
    }

    public GridBagLayoutConstraintBuilder fillHorizontal() {
      this.fill = GridBagConstraints.HORIZONTAL;
      return this;
    }

    public GridBagLayoutConstraintBuilder fillVertical() {
      this.fill = GridBagConstraints.VERTICAL;
      return this;
    }

    public GridBagLayoutConstraintBuilder fillBoth() {
      this.fill = GridBagConstraints.BOTH;
      return this;
    }

    public GridBagLayoutConstraintBuilder fillNone() {
      this.fill = GridBagConstraints.NONE;
      return this;
    }

    public GridBagLayoutConstraintBuilder ipadx(int ipadx) {
      this.ipadx = ipadx;
      return this;
    }

    public GridBagLayoutConstraintBuilder ipady(int ipady) {
      this.ipady = ipady;
      return this;
    }

    public GridBagLayoutConstraintBuilder insets(Insets insets) {
      this.insets = insets;
      return this;
    }
    
    public GridBagLayoutConstraintBuilder anchor(int anchor) {
      this.anchor = anchor;
      return this;
    }
    
    public GridBagLayoutConstraintBuilder weightx(int weightx) {
      this.weightx = weightx;
      return this;
    }
    
    public GridBagLayoutConstraintBuilder weighty(int weighty) {
      this.weighty = weighty;
      return this;
    }
    
    public static GridBagLayoutConstraintBuilder defaults() {
      GridBagLayoutConstraintBuilder builder = new GridBagLayoutConstraintBuilder();
      builder.gridx = GridBagConstraints.RELATIVE;
      builder.gridy = GridBagConstraints.RELATIVE;
      builder.gridwidth = 1;
      builder.gridheight = 1;
      
      builder.weightx = 0;
      builder.weighty = 0;
      builder.anchor = GridBagConstraints.CENTER;
      builder.fill = GridBagConstraints.NONE;

      builder.insets = new Insets(0, 0, 0, 0);
      builder.ipadx = 0;
      builder.ipady = 0;
      
      return builder;
    }
    
    public GridBagConstraints build() {
      return new GridBagConstraints(gridx, gridy, gridwidth, gridheight, weightx, weighty, anchor, fill, insets, ipadx, ipady);
    }
  }
}
