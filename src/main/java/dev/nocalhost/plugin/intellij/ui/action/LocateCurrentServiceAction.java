package dev.nocalhost.plugin.intellij.ui.action;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.actionSystem.AnActionEvent;

import org.jetbrains.annotations.NotNull;

import dev.nocalhost.plugin.intellij.service.NocalhostContextManager;
import dev.nocalhost.plugin.intellij.topic.NocalhostTreeExpandNotifier;

public class LocateCurrentServiceAction extends DumbAwareAction {
    private final Project project;

    public LocateCurrentServiceAction(Project project) {
        super("Locate Current Service", "", AllIcons.General.Locate);
        this.project = project;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        project
                .getMessageBus()
                .syncPublisher(NocalhostTreeExpandNotifier.NOCALHOST_TREE_EXPAND_NOTIFIER_TOPIC)
                .action();
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        var context = NocalhostContextManager.getInstance(project).getContext();
        e.getPresentation().setEnabled(context != null);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
