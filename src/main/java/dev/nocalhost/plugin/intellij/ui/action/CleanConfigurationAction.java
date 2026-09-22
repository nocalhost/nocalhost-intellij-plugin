package dev.nocalhost.plugin.intellij.ui.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageDialogBuilder;

import org.jetbrains.annotations.NotNull;

import dev.nocalhost.plugin.intellij.exception.NocalhostNotifier;
import dev.nocalhost.plugin.intellij.i18n.NocalhostI18n;
import dev.nocalhost.plugin.intellij.settings.NocalhostSettings;
import dev.nocalhost.plugin.intellij.topic.NocalhostTreeUpdateNotifier;

public class CleanConfigurationAction extends DumbAwareAction {
    private final NocalhostSettings nocalhostSettings = ApplicationManager.getApplication().getService(
            NocalhostSettings.class);

    private final Project project;

    public CleanConfigurationAction(Project project) {
        super(NocalhostI18n.get("action.cleanConfiguration"));
        this.project = project;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        if (!MessageDialogBuilder.yesNo(NocalhostI18n.get("action.cleanConfiguration"), NocalhostI18n.get("confirm.cleanConfiguration")).ask(project)) {
            return;
        }
        nocalhostSettings.cleanStandaloneCluster();
        nocalhostSettings.cleanNocalhostAccount();
        ApplicationManager.getApplication().getMessageBus().syncPublisher(
                NocalhostTreeUpdateNotifier.NOCALHOST_TREE_UPDATE_NOTIFIER_TOPIC).action();
        NocalhostNotifier.getInstance(project).notifySuccess(NocalhostI18n.get("success.cleanConfiguration"), NocalhostI18n.get("success.cleanConfiguration.content"));
    }
}
