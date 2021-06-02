package dev.nocalhost.plugin.intellij.ui.action.namespace;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.List;

import dev.nocalhost.plugin.intellij.commands.NhctlCommand;
import dev.nocalhost.plugin.intellij.commands.data.NhctlListPVCOptions;
import dev.nocalhost.plugin.intellij.commands.data.NhctlPVCItem;
import dev.nocalhost.plugin.intellij.ui.dialog.ClearPersistentDataDialog;
import dev.nocalhost.plugin.intellij.ui.tree.node.NamespaceNode;
import dev.nocalhost.plugin.intellij.utils.ErrorUtil;
import dev.nocalhost.plugin.intellij.utils.KubeConfigUtil;

public class CleanDevSpacePersistentDataAction extends DumbAwareAction {
    private final NhctlCommand nhctlCommand = ServiceManager.getService(NhctlCommand.class);

    private final Project project;
    private final Path kubeConfigPath;
    private final String namespace;

    public CleanDevSpacePersistentDataAction(Project project, NamespaceNode node) {
        super("Clear Persistent Data");
        this.project = project;
        this.kubeConfigPath = KubeConfigUtil.kubeConfigPath(node.getClusterNode().getRawKubeConfig());
        this.namespace = node.getNamespace();
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                NhctlListPVCOptions opts = new NhctlListPVCOptions(kubeConfigPath, namespace);
                List<NhctlPVCItem> nhctlPVCItems = nhctlCommand.listPVC(opts);
                ApplicationManager.getApplication().invokeLater(() -> {
                    ApplicationManager.getApplication().invokeLater(() -> {
                        new ClearPersistentDataDialog(project, kubeConfigPath, namespace, nhctlPVCItems).showAndGet();
                    });
                });
            } catch (Exception e) {
                ErrorUtil.dealWith(project, "Loading PVCs error",
                        "Error occurs while loading PVCs", e);
            }
        });

    }
}
