package dev.nocalhost.plugin.intellij.ui.action.workload;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.SystemInfo;

import org.jetbrains.annotations.NotNull;

import dev.nocalhost.plugin.intellij.nhctl.NhctlProxyCommand;
import dev.nocalhost.plugin.intellij.ui.console.NocalhostConsoleManager;
import dev.nocalhost.plugin.intellij.ui.dialog.SudoPasswordDialog;
import dev.nocalhost.plugin.intellij.ui.tree.node.ResourceNode;
import dev.nocalhost.plugin.intellij.utils.ErrorUtil;
import dev.nocalhost.plugin.intellij.utils.KubeConfigUtil;
import dev.nocalhost.plugin.intellij.utils.NhctlUtil;
import icons.NocalhostIcons;

public class ProxyConnectAction extends DumbAwareAction {
    private final Project project;
    private final ResourceNode node;

    public ProxyConnectAction(@NotNull Project project, @NotNull ResourceNode node) {
        super("Start ProxyMode", "", NocalhostIcons.VPN.Healthy);
        this.node = node;
        this.project = project;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        if (SystemInfo.isWindows) {
            execute(null);
            return;
        }
        var dialog = new SudoPasswordDialog(project, NhctlUtil.binaryPath());
        if (dialog.showAndGet()) {
            execute(dialog.getPassword());
        }
    }

    private void execute(String pwd) {
        NocalhostConsoleManager.activateOutputWindow(project);
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                var cmd = new NhctlProxyCommand(project);
                cmd.setAction("connect");
                cmd.setWorkload(node.getKubeResource().getKind() + "/" + node.resourceName());
                cmd.setNamespace(node.getNamespaceNode().getNamespace());
                cmd.setKubeConfig(KubeConfigUtil.kubeConfigPath(node.getClusterNode().getRawKubeConfig()));
                cmd.execute(pwd);
            } catch (Exception ex) {
                ErrorUtil.dealWith(project, "Failed to start proxy", "Error occurred while starting proxy.", ex);
            }
        });
    }
}
