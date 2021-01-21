package dev.nocalhost.plugin.intellij.ui.tree.node;

import javax.swing.tree.DefaultMutableTreeNode;

import dev.nocalhost.plugin.intellij.commands.data.KubeResource;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ResourceNode extends DefaultMutableTreeNode {
    private KubeResource kubeResource;

    public ResourceNode clone() {
        return new ResourceNode(kubeResource);
    }
}