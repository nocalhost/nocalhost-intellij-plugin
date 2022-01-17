package dev.nocalhost.plugin.intellij.ui.action.workload;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.List;

import dev.nocalhost.plugin.intellij.commands.NhctlCommand;
import dev.nocalhost.plugin.intellij.commands.data.NhctlListPVCOptions;
import dev.nocalhost.plugin.intellij.commands.data.NhctlPVCItem;
import dev.nocalhost.plugin.intellij.ui.dialog.ClearPersistentDataDialog;
import dev.nocalhost.plugin.intellij.ui.tree.node.ResourceNode;
import dev.nocalhost.plugin.intellij.utils.ErrorUtil;
import dev.nocalhost.plugin.intellij.utils.KubeConfigUtil;

public class ClearPersistentDataAction extends DumbAwareAction {
    private final NhctlCommand nhctlCommand = ApplicationManager.getApplication().getService(NhctlCommand.class);

    private final Project project;
    private final ResourceNode node;
    private final Path kubeConfigPath;
    private final String namespace;

    public ClearPersistentDataAction(Project project, ResourceNode node) {
        super("Clear PVC");
        this.project = project;
        this.node = node;
        this.kubeConfigPath = KubeConfigUtil.kubeConfigPath(node.getClusterNode().getRawKubeConfig());
        this.namespace = node.getNamespaceNode().getNamespace();
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                NhctlListPVCOptions opts = new NhctlListPVCOptions(kubeConfigPath, namespace);
                opts.setApp(node.applicationName());
                opts.setSvc(node.resourceName());
                opts.setControllerType(node.controllerType());
                List<NhctlPVCItem> nhctlPVCItems = nhctlCommand.listPVC(opts);
                ApplicationManager.getApplication().invokeLater(() -> {
                    var dialog = new ClearPersistentDataDialog(project, kubeConfigPath, namespace, nhctlPVCItems);
                    dialog.showAndGet();
                });
            } catch (Exception ex) {
                ErrorUtil.dealWith(project, "Failed to list PVC", "Error occurred while listing PVC", ex);
            }
        });

    }
}
