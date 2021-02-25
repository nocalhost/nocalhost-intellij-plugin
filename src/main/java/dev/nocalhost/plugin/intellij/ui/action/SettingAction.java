package dev.nocalhost.plugin.intellij.ui.action;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;

public class SettingAction extends AnAction implements DumbAware {

    public SettingAction() {
        super("Setting", "Setting Nocalhost", AllIcons.General.Settings);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        final Project project = e.getData(PlatformDataKeys.PROJECT);
        ShowSettingsUtil showSettingsUtil = ServiceManager.getService(ShowSettingsUtil.class);
        showSettingsUtil.showSettingsDialog(project, "Nocalhost");
    }
}
