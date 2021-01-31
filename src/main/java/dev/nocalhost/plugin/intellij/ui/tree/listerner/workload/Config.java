package dev.nocalhost.plugin.intellij.ui.tree.listerner.workload;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;

import org.apache.commons.io.FileUtils;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import dev.nocalhost.plugin.intellij.commands.NhctlCommand;
import dev.nocalhost.plugin.intellij.commands.data.NhctlConfigOptions;
import dev.nocalhost.plugin.intellij.ui.tree.node.ResourceNode;
import dev.nocalhost.plugin.intellij.utils.KubeConfigUtil;

public class Config implements ActionListener {
    private static final Logger LOG = Logger.getInstance(Config.class);

    private final ResourceNode node;
    private final Project project;

    public Config(ResourceNode node, Project project) {
        this.node = node;
        this.project = project;
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        final NhctlCommand nhctlCommand = ServiceManager.getService(NhctlCommand.class);

        NhctlConfigOptions nhctlConfigOptions = new NhctlConfigOptions();
        nhctlConfigOptions.setDeployment(node.resourceName());
        nhctlConfigOptions.setKubeconfig(KubeConfigUtil.kubeConfigPath(node.devSpace()).toString());
        try {
            String config = nhctlCommand.getConfig(node.devSpace().getContext().getApplicationName(), nhctlConfigOptions);
            File configFile = File.createTempFile(String.format("%s-%s-%s-", node.devSpace().getNamespace(), node.devSpace().getContext().getApplicationName(), node.resourceName()), ".yaml");
            FileUtils.write(configFile, config, Charset.defaultCharset());
            VirtualFile virtualFile = LocalFileSystem.getInstance().findFileByIoFile(configFile);
            if (null != virtualFile) {
                FileEditorManager.getInstance(project).openTextEditor(new OpenFileDescriptor(project, virtualFile, 0), true);
            }
        } catch (IOException | InterruptedException e) {
            LOG.error("error occurred while getting application config", e);
        }
    }
}
