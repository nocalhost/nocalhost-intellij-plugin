package dev.nocalhost.plugin.intellij.ui.action.application;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
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
import dev.nocalhost.plugin.intellij.ui.tree.node.ApplicationNode;

public class ClearAppPersisentDataAction extends AnAction {
    private static final Logger LOG = Logger.getInstance(ClearAppPersisentDataAction.class);

    final NhctlCommand nhctlCommand = ServiceManager.getService(NhctlCommand.class);

    private final Project project;
    private final ApplicationNode node;

    public ClearAppPersisentDataAction(Project project, ApplicationNode node) {
        super("Clear Persistent Data");
        this.project = project;
        this.node = node;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            NhctlListPVCOptions opts = new NhctlListPVCOptions(node.getDevSpace());
            opts.setApp(node.getApplication().getContext().getApplicationName());
            try {
                List<NhctlPVCItem> nhctlPVCItems = nhctlCommand.listPVC(opts);
                ApplicationManager.getApplication().invokeLater(() -> {
                    new ClearPersistentDataDialog(project, node.getDevSpace(), nhctlPVCItems, true)
                            .showAndGet();
                });
            } catch (IOException | InterruptedException | NocalhostExecuteCmdException e) {
                LOG.error("error occurred while listing pvc items", e);
            }
        });
    }
}
