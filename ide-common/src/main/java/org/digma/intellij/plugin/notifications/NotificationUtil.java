package org.digma.intellij.plugin.notifications;

import com.intellij.notification.*;
import com.intellij.openapi.project.Project;
import org.digma.intellij.plugin.common.EDT;

public class NotificationUtil {

    public static final String DIGMA_HIDDEN_NOTIFICATION_GROUP = "Digma Hidden Notification Group";
    public static final String DIGMA_STICKY_BALLOON_NOTIFICATION_GROUP = "Digma Sticky Balloon Notification Group";
    public static final String DIGMA_FADING_BALLOON_NOTIFICATION_GROUP = "Digma fading Balloon Notification Group";

    public static void notifyError(Project project, String content) {

        EDT.ensureEDT(() -> NotificationGroupManager.getInstance()
                .getNotificationGroup(DIGMA_HIDDEN_NOTIFICATION_GROUP)
                .createNotification(content, NotificationType.ERROR)
                .notify(project));
    }

    public static void notifyWarning(Project project, String content) {

        EDT.ensureEDT(() ->
                NotificationGroupManager.getInstance()
                        .getNotificationGroup(DIGMA_HIDDEN_NOTIFICATION_GROUP)
                        .createNotification(content, NotificationType.WARNING)
                        .notify(project));
    }

    public static void notifyFadingError(Project project, String content) {

        EDT.ensureEDT(() ->
                NotificationGroupManager.getInstance()
                        .getNotificationGroup(DIGMA_FADING_BALLOON_NOTIFICATION_GROUP)
                        .createNotification(content, NotificationType.ERROR)
                        .notify(project));
    }

    public static void notifyFadingInfo(Project project, String content) {

        EDT.ensureEDT(() ->
                NotificationGroupManager.getInstance()
                        .getNotificationGroup(DIGMA_FADING_BALLOON_NOTIFICATION_GROUP)
                        .createNotification(content, NotificationType.INFORMATION)
                        .notify(project));
    }

    public static void notifyChangingEnvironment(Project project, String oldEnv, String newEnv) {

        EDT.ensureEDT(() -> {
            var content = "Digma: Changing environment " + oldEnv + " to " + newEnv;
            NotificationGroupManager.getInstance()
                    .getNotificationGroup(DIGMA_HIDDEN_NOTIFICATION_GROUP)
                    .createNotification(content, NotificationType.INFORMATION)
                    .notify(project);
        });
    }


    public static void showNotification(Project project, String content) {

        var myContent = content.startsWith("Digma:") ? content : "Digma: " + content;
        EDT.ensureEDT(() -> NotificationGroupManager.getInstance()
                .getNotificationGroup(DIGMA_HIDDEN_NOTIFICATION_GROUP)
                .createNotification(myContent, NotificationType.INFORMATION)
                .notify(project));
    }


    public static void showBalloonWarning(Project project, String content) {

        EDT.ensureEDT(() ->
                NotificationGroupManager.getInstance()
                        .getNotificationGroup(DIGMA_STICKY_BALLOON_NOTIFICATION_GROUP)
                        .createNotification(content, NotificationType.WARNING)
                        .notify(project));
    }

    public static void showBalloonWarning(Project project, String title, String content) {

        EDT.ensureEDT(() ->
                NotificationGroupManager.getInstance()
                        .getNotificationGroup(DIGMA_STICKY_BALLOON_NOTIFICATION_GROUP)
                        .createNotification(title, content, NotificationType.WARNING)
                        .notify(project));
    }


}
