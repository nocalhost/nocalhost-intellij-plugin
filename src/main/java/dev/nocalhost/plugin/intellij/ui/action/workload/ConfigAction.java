package dev.nocalhost.plugin.intellij.ui.action.workload;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import dev.nocalhost.plugin.intellij.commands.NhctlCommand;
import dev.nocalhost.plugin.intellij.commands.data.NhctlConfigOptions;
import dev.nocalhost.plugin.intellij.exception.NocalhostExecuteCmdException;
import dev.nocalhost.plugin.intellij.ui.tree.node.ResourceNode;
import dev.nocalhost.plugin.intellij.ui.vfs.ConfigFile;
import dev.nocalhost.plugin.intellij.utils.KubeConfigUtil;

public class ConfigAction extends AnAction {
    private static final Logger LOG = Logger.getInstance(ConfigAction.class);

    private final Project project;
    private final ResourceNode node;

    public ConfigAction(Project project, ResourceNode node) {
        super("Config", "", AllIcons.Nodes.Editorconfig);
        this.project = project;
        this.node = node;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        final NhctlCommand nhctlCommand = ServiceManager.getService(NhctlCommand.class);

        NhctlConfigOptions nhctlConfigOptions = new NhctlConfigOptions();
        nhctlConfigOptions.setDeployment(node.resourceName());
        nhctlConfigOptions.setKubeconfig(KubeConfigUtil.kubeConfigPath(node.devSpace()).toString());
        try {
            String config = nhctlCommand.getConfig(node.devSpace().getContext().getApplicationName(), nhctlConfigOptions);
            String filename = node.resourceName() + ".yaml";
            VirtualFile virtualFile = new ConfigFile(filename, filename, config, project, node);
            OpenFileDescriptor openFileDescriptor = new OpenFileDescriptor(project, virtualFile, 0);
            FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
            fileEditorManager.openTextEditor(openFileDescriptor, true);
        } catch (IOException | InterruptedException | NocalhostExecuteCmdException e) {
            LOG.error("error occurred while getting application config", e);
        }
    }
}
