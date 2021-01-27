package dev.nocalhost.plugin.intellij.ui.tree.listerner.devspace;

import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import dev.nocalhost.plugin.intellij.api.data.DevSpace;
import dev.nocalhost.plugin.intellij.ui.tree.node.DevSpaceNode;
import dev.nocalhost.plugin.intellij.ui.vfs.ReadOnlyVirtualFile;

public class ViewKubeConfig implements ActionListener {
    private final DevSpaceNode node;
    private final Project project;

    public ViewKubeConfig(DevSpaceNode node, Project project) {
        this.node = node;
        this.project = project;
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        final DevSpace devSpace = node.getDevSpace();
        final String filename = devSpace.getSpaceName() + ".yaml";
        VirtualFile virtualFile = new ReadOnlyVirtualFile(filename, filename, devSpace.getKubeConfig());
        FileEditorManager.getInstance(project).openTextEditor(new OpenFileDescriptor(project, virtualFile, 0), true);
    }
}
