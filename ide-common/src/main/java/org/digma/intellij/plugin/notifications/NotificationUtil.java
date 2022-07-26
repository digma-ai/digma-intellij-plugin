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

    public static void notifyChangingEnvironment(Project project, String oldEnv,String newEnv) {

        var content = "Digma: Changing environment " + oldEnv + " to " + newEnv;
        NotificationGroupManager.getInstance()
                .getNotificationGroup(NOTIFICATION_GROUP)
                .createNotification(content, NotificationType.INFORMATION)
                .notify(project);
    }


    public static void showNotification(Project project,String content) {

        content = content.startsWith("content") ? content : "Digma: " +content;
        NotificationGroupManager.getInstance()
                .getNotificationGroup(NOTIFICATION_GROUP)
                .createNotification(content, NotificationType.INFORMATION)
                .notify(project);
    }


}
