package dev.nocalhost.plugin.intellij.ui.tree.node;

import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;

import dev.nocalhost.plugin.intellij.settings.data.NocalhostAccount;
import icons.NocalhostIcons;

import lombok.Getter;

@Getter
public class AccountNode extends DefaultMutableTreeNode {
    private final NocalhostAccount account;

    public AccountNode(@NotNull NocalhostAccount na) {
        this.account = na;
    }

    public Icon getIcon() {
        return NocalhostIcons.ClusterWarning;
    }

    public String getName() {
        return "unknown";
    }

    public String getTooltip() {
        return account.getUsername() + " [" + account.getServer() + "]";
    }
}
