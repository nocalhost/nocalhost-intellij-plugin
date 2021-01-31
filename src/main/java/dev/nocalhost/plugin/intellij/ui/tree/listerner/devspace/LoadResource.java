package dev.nocalhost.plugin.intellij.ui.tree.listerner.devspace;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import dev.nocalhost.plugin.intellij.api.data.DevSpace;
import dev.nocalhost.plugin.intellij.commands.NhctlCommand;
import dev.nocalhost.plugin.intellij.commands.data.NhctlDescribeOptions;
import dev.nocalhost.plugin.intellij.ui.tree.node.DevSpaceNode;
import dev.nocalhost.plugin.intellij.ui.vfs.ReadOnlyVirtualFile;
import dev.nocalhost.plugin.intellij.utils.KubeConfigUtil;

public class LoadResource implements ActionListener {
    private static final Logger LOG = Logger.getInstance(LoadResource.class);

    private final DevSpaceNode node;
    private final Project project;

    public LoadResource(DevSpaceNode node, Project project) {
        this.node = node;
        this.project = project;
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        final DevSpace devSpace = node.getDevSpace();
        final NhctlCommand nhctlCommand = ServiceManager.getService(NhctlCommand.class);

        NhctlDescribeOptions opts = new NhctlDescribeOptions();
        opts.setKubeconfig(KubeConfigUtil.kubeConfigPath(devSpace).toString());
        try {
            String resource = nhctlCommand.describe(devSpace.getContext().getApplicationName(), opts);
            String filename = devSpace.getContext().getApplicationName() + ".yaml";
            VirtualFile virtualFile = new ReadOnlyVirtualFile(filename, filename, resource);
            FileEditorManager.getInstance(project).openTextEditor(new OpenFileDescriptor(project, virtualFile, 0), true);
        } catch (IOException | InterruptedException e) {
            LOG.error("error occurred while describing application", e);
        }
    }
}
