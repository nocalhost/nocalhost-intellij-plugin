package dev.nocalhost.plugin.intellij.ui.action;

import com.google.inject.Inject;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;

import org.jetbrains.annotations.NotNull;

import dev.nocalhost.plugin.intellij.NocalhostModule;
import dev.nocalhost.plugin.intellij.commands.NhctlClient;
import lombok.SneakyThrows;

public class RefreshAction extends AnAction implements DumbAware {

    public RefreshAction() {
        super("Refresh", "Refresh nocalhost data", AllIcons.Actions.Refresh);
    }

    @SneakyThrows
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        NhctlClient nhctlClient = NocalhostModule.getInstance(NhctlClient.class);
        Project project = e.getData(PlatformDataKeys.PROJECT);
        String title = "Nocalhost Plugin";
        String msg = "Refresh data!";
        String nhctlVersion = nhctlClient.version();

        Messages.showMessageDialog(project, msg + "\n" + nhctlVersion, title, null);
    }
}
