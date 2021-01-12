package dev.nocalhost.plugin.intellij.ui.tree;

import lombok.Getter;

@Getter
public class ConfigurationNode {
    private String name;

    public ConfigurationNode(String name) {
        this.name = name;
    }
}
