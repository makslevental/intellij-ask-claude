package dev.makslevental.claudeask;

import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.Project;

/** Balloon notifications. Deliberately free of terminal classes, so it works when the Terminal plugin is missing. */
final class Notices {
    private Notices() {}

    static void show(Project project, String text, NotificationType type) {
        NotificationGroupManager.getInstance().getNotificationGroup("Ask Claude")
                .createNotification(text, type).notify(project);
    }
}
