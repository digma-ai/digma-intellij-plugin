package org.digma.intellij.plugin.notifications;

import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.Project;

public class NotificationUtil {

    public static final String NOTIFICATION_GROUP = "Digma Notification Group";

    public static void notifyError(Project project, String content) {

        NotificationGroupManager.getInstance()
                .getNotificationGroup(NOTIFICATION_GROUP)
                .createNotification(content, NotificationType.ERROR)
                .notify(project);
    }
}
