package dev.nocalhost.plugin.intellij.ui.action.cluster;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

import org.jetbrains.annotations.NotNull;

import dev.nocalhost.plugin.intellij.ui.tree.node.ClusterNode;
import dev.nocalhost.plugin.intellij.ui.vfs.ReadOnlyVirtualFile;

public class ViewClusterKubeConfigAction extends DumbAwareAction {
    private final Project project;
    private final ClusterNode node;

    public ViewClusterKubeConfigAction(Project project, ClusterNode node) {
        super("View KubeConfig");
        this.project = project;
        this.node = node;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        final String filename = node.getKubeConfig().getContexts().get(0).getName() + ".yaml";
        VirtualFile virtualFile = new ReadOnlyVirtualFile(filename, filename,
                node.getRawKubeConfig());
        FileEditorManager.getInstance(project).openTextEditor(
                new OpenFileDescriptor(project, virtualFile, 0), true);
    }
}
