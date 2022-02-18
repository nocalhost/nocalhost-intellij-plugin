package dev.nocalhost.plugin.intellij.ui.action.workload;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;

import dev.nocalhost.plugin.intellij.nhctl.NhctlProxyCommand;
import dev.nocalhost.plugin.intellij.ui.console.NocalhostConsoleManager;
import dev.nocalhost.plugin.intellij.ui.tree.node.ResourceNode;
import dev.nocalhost.plugin.intellij.utils.ErrorUtil;
import dev.nocalhost.plugin.intellij.utils.KubeConfigUtil;
import icons.NocalhostIcons;

public class ProxyReconnectAction extends DumbAwareAction {
    private final Project project;
    private final ResourceNode node;

    public ProxyReconnectAction(@NotNull Project project, @NotNull ResourceNode node) {
        super("Resume ProxyMode", "", NocalhostIcons.VPN.Unhealthy);
        this.node = node;
        this.project = project;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        NocalhostConsoleManager.activateOutputWindow(project);
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                var cmd = new NhctlProxyCommand(project);
                cmd.setAction("reconnect");
                cmd.setWorkload(node.controllerType() + "/" + node.resourceName());
                cmd.setNamespace(node.getNamespaceNode().getNamespace());
                cmd.setKubeConfig(KubeConfigUtil.toPath(node.getClusterNode().getRawKubeConfig()));
                cmd.execute();
            } catch (Exception ex) {
                ErrorUtil.dealWith(project, "Failed to resume proxy", "Error occurred while resuming proxy.", ex);
            }
        });
    }
}
