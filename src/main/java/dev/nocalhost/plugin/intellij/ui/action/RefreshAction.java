package dev.nocalhost.plugin.intellij.ui.action;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbAware;

import org.jetbrains.annotations.NotNull;

import dev.nocalhost.plugin.intellij.topic.DevSpaceListUpdatedNotifier;

public class RefreshAction extends AnAction implements DumbAware {

    public RefreshAction() {
        super("Refresh", "Refresh nocalhost data", AllIcons.Actions.Refresh);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        final Application application = ApplicationManager.getApplication();
        DevSpaceListUpdatedNotifier publisher = application.getMessageBus()
                                                           .syncPublisher(DevSpaceListUpdatedNotifier.DEV_SPACE_LIST_UPDATED_NOTIFIER_TOPIC);
        publisher.action();
    }
}
