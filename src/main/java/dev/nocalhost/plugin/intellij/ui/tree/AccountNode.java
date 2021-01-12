package dev.nocalhost.plugin.intellij.ui.tree;

import lombok.Getter;

@Getter
public class AccountNode {
    private String name;

    public AccountNode(String name) {
        this.name = name;
    }
}
