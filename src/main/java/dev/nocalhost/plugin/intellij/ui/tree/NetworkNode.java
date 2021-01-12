package dev.nocalhost.plugin.intellij.ui.tree;

import lombok.Getter;

@Getter
public class NetworkNode {
    private String name;

    public NetworkNode(String name) {
        this.name = name;
    }
}
