package dev.nocalhost.plugin.intellij.ui.action.application;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import dev.nocalhost.plugin.intellij.commands.NhctlCommand;
import dev.nocalhost.plugin.intellij.commands.data.NhctlListPVCOptions;
import dev.nocalhost.plugin.intellij.commands.data.NhctlPVCItem;
import dev.nocalhost.plugin.intellij.exception.NocalhostExecuteCmdException;
import dev.nocalhost.plugin.intellij.ui.dialog.ClearPersistentDataDialog;
import dev.nocalhost.plugin.intellij.ui.tree.node.ApplicationNode;
import dev.nocalhost.plugin.intellij.utils.KubeConfigUtil;

public class ClearAppPersisentDataAction extends DumbAwareAction {
    private static final Logger LOG = Logger.getInstance(ClearAppPersisentDataAction.class);

    private final NhctlCommand nhctlCommand = ServiceManager.getService(NhctlCommand.class);

    private final Project project;
    private final Path kubeConfigPath;
    private final String namespace;
    private final String applicationName;

    public ClearAppPersisentDataAction(Project project, ApplicationNode node) {
        super("Clear Persistent Data");
        this.project = project;
        this.kubeConfigPath = KubeConfigUtil.kubeConfigPath(node.getClusterNode().getRawKubeConfig());
        this.namespace = node.getNamespaceNode().getNamespace();
        this.applicationName = node.getName();
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                NhctlListPVCOptions opts = new NhctlListPVCOptions(kubeConfigPath, namespace);
                opts.setApp(applicationName);
                List<NhctlPVCItem> nhctlPVCItems = nhctlCommand.listPVC(opts);
                ApplicationManager.getApplication().invokeLater(() -> {
                    new ClearPersistentDataDialog(project, kubeConfigPath, namespace, nhctlPVCItems).showAndGet();
                });
            } catch (IOException | InterruptedException | NocalhostExecuteCmdException e) {
                LOG.error("error occurred while listing pvc items", e);
            }
        });
    }
}
