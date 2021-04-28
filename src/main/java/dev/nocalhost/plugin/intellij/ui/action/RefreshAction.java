package dev.nocalhost.plugin.intellij.ui.action;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbAware;

import org.jetbrains.annotations.NotNull;

import dev.nocalhost.plugin.intellij.topic.NocalhostTreeDataUpdateNotifier;

public class RefreshAction extends AnAction implements DumbAware {

    public RefreshAction() {
        super("Refresh", "Refresh nocalhost data", AllIcons.Actions.Refresh);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        ApplicationManager.getApplication().getMessageBus().syncPublisher(
                NocalhostTreeDataUpdateNotifier.NOCALHOST_TREE_DATA_UPDATE_NOTIFIER_TOPIC
        ).action();
    }
}
