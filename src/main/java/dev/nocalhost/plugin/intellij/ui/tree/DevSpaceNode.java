package dev.nocalhost.plugin.intellij.ui.tree;

import dev.nocalhost.plugin.intellij.api.data.DevSpace;
import lombok.Getter;

@Getter
public class DevSpaceNode {
    private DevSpace devSpace;

    public DevSpaceNode(DevSpace devSpace) {
        this.devSpace = devSpace;
    }
}
