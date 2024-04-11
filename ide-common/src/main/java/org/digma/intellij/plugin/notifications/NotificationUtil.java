package org.digma.intellij.plugin.notifications;

import com.intellij.notification.*;
import com.intellij.openapi.project.Project;

public class NotificationUtil {

    public static final String DIGMA_HIDDEN_NOTIFICATION_GROUP = "Digma Hidden Notification Group";
    public static final String DIGMA_STICKY_BALLOON_NOTIFICATION_GROUP = "Digma Sticky Balloon Notification Group";
    public static final String DIGMA_FADING_BALLOON_NOTIFICATION_GROUP = "Digma fading Balloon Notification Group";

    public static void notifyError(Project project, String content) {

        NotificationGroupManager.getInstance()
                .getNotificationGroup(DIGMA_HIDDEN_NOTIFICATION_GROUP)
                .createNotification(content, NotificationType.ERROR)
                .notify(project);
    }

    public static void notifyFadingError(Project project, String content) {

        NotificationGroupManager.getInstance()
                .getNotificationGroup(DIGMA_FADING_BALLOON_NOTIFICATION_GROUP)
                .createNotification(content, NotificationType.ERROR)
                .notify(project);
    }

    public static void notifyChangingEnvironment(Project project, String oldEnv,String newEnv) {

        var content = "Digma: Changing environment " + oldEnv + " to " + newEnv;
        NotificationGroupManager.getInstance()
                .getNotificationGroup(DIGMA_HIDDEN_NOTIFICATION_GROUP)
                .createNotification(content, NotificationType.INFORMATION)
                .notify(project);
    }


    public static void showNotification(Project project,String content) {

        content = content.startsWith("Digma:") ? content : "Digma: " + content;
        NotificationGroupManager.getInstance()
                .getNotificationGroup(DIGMA_HIDDEN_NOTIFICATION_GROUP)
                .createNotification(content, NotificationType.INFORMATION)
                .notify(project);
    }


    public static void showBalloonWarning(Project project, String content) {
        NotificationGroupManager.getInstance()
                .getNotificationGroup(DIGMA_STICKY_BALLOON_NOTIFICATION_GROUP)
                .createNotification(content, NotificationType.WARNING)
                .notify(project);
    }

    public static void showBalloonWarning(Project project, String title, String content) {
        NotificationGroupManager.getInstance()
                .getNotificationGroup(DIGMA_STICKY_BALLOON_NOTIFICATION_GROUP)
                .createNotification(title, content, NotificationType.WARNING)
                .notify(project);
    }


}
