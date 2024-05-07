package com.oracle.oci.intellij.util.fills;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationListener;
import com.intellij.notification.NotificationType;

@Shim(forType = NotificationGroup.class)
public class NotificationGroupShim {

  private NotificationGroup notificationGroup;

  public NotificationGroupShim(NotificationGroup notificationGroup) {
    this.notificationGroup = notificationGroup;
  }

  public Notification createNotification(String title, String content, NotificationType type) {
    return createNotification(title, null, content, null);
  }

  @ShimMethod(methodName = "createNotification()")
  public Notification createNotification(String title,
                                         String subtitle,
                                         String content,
                                         NotificationType type) {

    return createNotification(title, subtitle, content, type, null);
  }
  
  @ShimMethod(methodName = "createNotification()")
  public Notification createNotification(String title,
                                         String subtitle,
                                         String content,
                                         NotificationType type,
                                         NotificationListener listener) {
    Notification notification = notificationGroup.createNotification(content, type);
    return notification.setTitle(title).setSubtitle(subtitle).setListener(listener);
  }
}
