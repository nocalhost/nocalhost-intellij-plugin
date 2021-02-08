package dev.nocalhost.plugin.intellij.ui.action.workload;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;

import dev.nocalhost.plugin.intellij.commands.NhctlCommand;
import dev.nocalhost.plugin.intellij.commands.data.NhctlListPVCOptions;
import dev.nocalhost.plugin.intellij.commands.data.NhctlPVCItem;
import dev.nocalhost.plugin.intellij.exception.NocalhostExecuteCmdException;
import dev.nocalhost.plugin.intellij.ui.ClearPersistentDataDialog;
import dev.nocalhost.plugin.intellij.ui.tree.node.ResourceNode;
import dev.nocalhost.plugin.intellij.utils.KubeConfigUtil;

public class ClearPersistentDataAction extends AnAction {
    private static final Logger LOG = Logger.getInstance(ClearPersistentDataAction.class);

    private final Project project;
    private final ResourceNode node;

    public ClearPersistentDataAction(Project project, ResourceNode node) {
        super("Clear Persistent Data");
        this.project = project;
        this.node = node;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        final NhctlCommand nhctlCommand = ServiceManager.getService(NhctlCommand.class);

        NhctlListPVCOptions opts = new NhctlListPVCOptions();
        opts.setApp(node.devSpace().getContext().getApplicationName());
        opts.setSvc(node.getNhctlSvcProfile().getName());
        opts.setKubeconfig(KubeConfigUtil.kubeConfigPath(node.devSpace()).toString());
        try {
            List<NhctlPVCItem> nhctlPVCItems = nhctlCommand.listPVC(opts);
            new ClearPersistentDataDialog(project, node.devSpace(), nhctlPVCItems, false).showAndGet();
        } catch (IOException | InterruptedException | NocalhostExecuteCmdException e) {
            LOG.error("error occurred while listing pvc", e);
        }
    }
}
