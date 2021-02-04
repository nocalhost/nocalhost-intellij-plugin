package dev.nocalhost.plugin.intellij.ui.action.devspace;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import dev.nocalhost.plugin.intellij.api.data.DevSpace;
import dev.nocalhost.plugin.intellij.helpers.NhctlHelper;
import dev.nocalhost.plugin.intellij.ui.InstallDevSpaceDialog;
import dev.nocalhost.plugin.intellij.ui.tree.node.DevSpaceNode;

public class InstallAppAction extends AnAction {
    private static final Logger LOG = Logger.getInstance(InstallAppAction.class);

    private final Project project;
    private final DevSpaceNode node;

    public InstallAppAction(Project project, DevSpaceNode node) {
        super("Install App", "", AllIcons.Actions.Install);
        this.project = project;
        this.node = node;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        final DevSpace devSpace = node.getDevSpace();

        try {
            if (NhctlHelper.isApplicationInstalled(devSpace)) {
                Messages.showMessageDialog("Application has been installed.", "Install application", null);
                return;
            }
        } catch (IOException | InterruptedException e) {
            LOG.error("error occurred while checking if application was installed", e);
            return;
        }

        new InstallDevSpaceDialog(project, node.getDevSpace()).showAndGet();
    }
}
