package dev.nocalhost.plugin.intellij.ui.tree;

import lombok.Getter;

@Getter
public class PlainNode {
    private String name;

    public PlainNode(String name) {
        this.name = name;
    }
}
