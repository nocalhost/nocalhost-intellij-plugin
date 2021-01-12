package dev.nocalhost.plugin.intellij.ui.tree;

import com.intellij.ide.util.treeView.NodeDescriptor;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.Nullable;

public class TreeNodeDescriptor extends NodeDescriptor<TreeModel> {

    public TreeNodeDescriptor(@Nullable Project project, @Nullable NodeDescriptor<?> parentDescriptor) {
        super(project, parentDescriptor);
    }

    @Override
    public boolean update() {
        return false;
    }

    @Override
    public TreeModel getElement() {
        return null;
    }
}
