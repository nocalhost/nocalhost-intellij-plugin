package dev.nocalhost.plugin.intellij.ui.action.devspace;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

import org.jetbrains.annotations.NotNull;

import dev.nocalhost.plugin.intellij.api.data.DevSpace;
import dev.nocalhost.plugin.intellij.ui.tree.node.DevSpaceNode;
import dev.nocalhost.plugin.intellij.ui.vfs.ReadOnlyVirtualFile;

public class ViewKubeConfigAction extends AnAction {
    private final Project project;
    private final DevSpaceNode node;

    public ViewKubeConfigAction(Project project, DevSpaceNode node) {
        super("View KubeConfig");
        this.project = project;
        this.node = node;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        final DevSpace devSpace = node.getDevSpace();
        final String filename = devSpace.getSpaceName() + ".yaml";
        VirtualFile virtualFile = new ReadOnlyVirtualFile(filename, filename, devSpace.getKubeConfig());
        FileEditorManager.getInstance(project).openTextEditor(new OpenFileDescriptor(project, virtualFile, 0), true);
    }
}
