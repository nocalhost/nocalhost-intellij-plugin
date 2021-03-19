package dev.nocalhost.plugin.intellij.ui.tree.node;

import javax.swing.tree.DefaultMutableTreeNode;

import dev.nocalhost.plugin.intellij.api.data.Application;
import dev.nocalhost.plugin.intellij.api.data.DevSpace;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ApplicationNode extends DefaultMutableTreeNode {
    private Application application;
    private DevSpace devSpace;
    private boolean expanded;
    private boolean installed;

    public ApplicationNode(Application application, DevSpace devSpace) {
        this(application, devSpace, false, false);
    }

    @Override
    public boolean isLeaf() {
        return !installed;
    }

    public ApplicationNode clone() {
        return new ApplicationNode(application, devSpace, expanded, installed);
    }
}
