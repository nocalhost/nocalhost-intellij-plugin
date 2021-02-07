package dev.nocalhost.plugin.intellij;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationDisplayType;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationListener;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsContexts;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.event.HyperlinkEvent;

import dev.nocalhost.plugin.intellij.topic.NocalhostExceptionPrintNotifier;
import icons.NocalhostIcons;

public class NocalhostNotifier {

    public static final String NOCALHOST_NOTIFICATION_ID = "Nocalhost.Notification";
    public static final String NOCALHOST_ERROR_NOTIFICATION_ID = "Nocalhost.Notification.Error";

    public static final NotificationGroup NOCALHOST_NOTIFICATION =
            NotificationGroup.create(
                    "Nocalhost.Notification", NotificationDisplayType.BALLOON, false, "Nocalhost",
                    NocalhostIcons.Logo, "Nocalhost", PluginId.getId("dev.nocalhost.nocalhost-intellij-plugin"));
    public static final NotificationGroup NOCALHOST_ERROR_NOTIFICATION =
            NotificationGroup.create(
                    "Nocalhost.Notification.Error", NotificationDisplayType.STICKY_BALLOON, true, "Nocalhost",
                    NocalhostIcons.Logo, "Nocalhost", PluginId.getId("dev.nocalhost.nocalhost-intellij-plugin"));

    private final Project project;

    public NocalhostNotifier(Project project) {
        this.project = project;
    }

    public static NocalhostNotifier getInstance(Project project) {
        return project.getService(NocalhostNotifier.class);
    }

    @NotNull
    public Notification notify(@NotNull Notification notification) {
        notification.notify(project);
        return notification;
    }

    @NotNull
    public Notification notifyError(@NlsContexts.NotificationTitle @NotNull String title,
                                    @NlsContexts.NotificationContent @NotNull String message) {
        return notify(NOCALHOST_ERROR_NOTIFICATION, NOCALHOST_ERROR_NOTIFICATION_ID, title, message, NotificationType.ERROR, null);
    }

    @NotNull
    public Notification notifyError(@NlsContexts.NotificationTitle @NotNull String title,
                                    @NlsContexts.NotificationContent @NotNull String message,
                                    @NotNull String eMessage) {
        String content = String.format("<html>%s<a href=\"nocalhost.show\">Show More</a></html>", message);
        return notify(NOCALHOST_ERROR_NOTIFICATION, NOCALHOST_ERROR_NOTIFICATION_ID, title, content, NotificationType.ERROR, new NotificationListener.Adapter() {
            @Override
            protected void hyperlinkActivated(@NotNull Notification notification, @NotNull HyperlinkEvent e) {
                ApplicationManager.getApplication().getMessageBus().syncPublisher(NocalhostExceptionPrintNotifier.NOCALHOST_CONSOLE_EXECUTE_NOTIFIER_TOPIC)
                                  .action(title, message, eMessage);
            }
        });
    }

    @NotNull
    public Notification notifyError(@NlsContexts.NotificationTitle @NotNull String title,
                                    @NlsContexts.NotificationContent @NotNull String message,
                                    @Nullable NotificationListener listener) {
        return notify(NOCALHOST_ERROR_NOTIFICATION, NOCALHOST_ERROR_NOTIFICATION_ID, title, message, NotificationType.ERROR, listener);
    }

    @NotNull
    public Notification notifySuccess(@NlsContexts.NotificationTitle @NotNull String title,
                                      @NlsContexts.NotificationContent @NotNull String message) {
        return notify(NOCALHOST_NOTIFICATION, NOCALHOST_NOTIFICATION_ID, title, message, NotificationType.INFORMATION, null);
    }

    @NotNull
    public Notification notifySuccess(@NlsContexts.NotificationTitle @NotNull String title,
                                      @NlsContexts.NotificationContent @NotNull String message,
                                      @Nullable NotificationListener listener) {
        return notify(NOCALHOST_NOTIFICATION, NOCALHOST_NOTIFICATION_ID, title, message, NotificationType.INFORMATION, listener);
    }

    @NotNull
    private Notification notify(@NotNull NotificationGroup notificationGroup,
                                @NonNls @Nullable String displayId,
                                @NlsContexts.NotificationTitle @NotNull String title,
                                @NlsContexts.NotificationContent @NotNull String message,
                                @NotNull NotificationType type,
                                @Nullable NotificationListener listener) {
        Notification notification = createNotification(notificationGroup, displayId, title, message, type, listener);
        return notify(notification);
    }

    private static Notification createNotification(@NotNull NotificationGroup notificationGroup,
                                                   @NonNls @Nullable String displayId,
                                                   @NlsContexts.NotificationTitle @NotNull String title,
                                                   @NlsContexts.NotificationContent @NotNull String message,
                                                   @NotNull NotificationType type,
                                                   @Nullable NotificationListener listener) {

        if (StringUtils.isBlank(message)) {
            message = title;
            title = "";
        }
        return notificationGroup.createNotification(title, message, type, listener, StringUtils.trimToEmpty(displayId));
    }
}
