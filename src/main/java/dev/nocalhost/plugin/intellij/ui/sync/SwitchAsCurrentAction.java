package dev.nocalhost.plugin.intellij.ui.sync;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;

import dev.nocalhost.plugin.intellij.service.NocalhostContextManager;
import dev.nocalhost.plugin.intellij.commands.data.NhctlDevAssociateQueryResult;
import dev.nocalhost.plugin.intellij.i18n.NocalhostI18n;

public class SwitchAsCurrentAction extends DumbAwareAction {
    private final Project project;
    private final NhctlDevAssociateQueryResult result;

    public SwitchAsCurrentAction(@NotNull Project project, @NotNull NhctlDevAssociateQueryResult result) {
        super(NocalhostI18n.get("action.switchCurrentService"));
        this.result = result;
        this.project = project;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        NocalhostContextManager.getInstance(project).replace(result);
    }
}
