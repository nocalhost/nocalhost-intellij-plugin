package dev.nocalhost.plugin.intellij.ui.tree;

import lombok.Getter;

@Getter
public class StorageNode {
    private String name;

    public StorageNode(String name) {
        this.name = name;
    }
}
