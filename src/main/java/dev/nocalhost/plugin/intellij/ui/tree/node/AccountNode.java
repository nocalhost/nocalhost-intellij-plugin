package dev.nocalhost.plugin.intellij.ui.tree.node;

import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;

import dev.nocalhost.plugin.intellij.settings.data.NocalhostAccount;
import icons.NocalhostIcons;

import lombok.Getter;
import lombok.Setter;

@Getter
public class AccountNode extends DefaultMutableTreeNode {
    private String extra;
    private final NocalhostAccount account;

    public AccountNode(@NotNull NocalhostAccount na, @NotNull String extra) {
        this.extra = extra;
        this.account = na;
    }

    public Icon getIcon() {
        return NocalhostIcons.ClusterWarning;
    }

    public String getName() {
        return account.getUsername();
    }

    public void updateFrom(AccountNode other) {
        extra = other.getExtra();
    }

    public String getTooltip() {
        return account.getUsername() + " [" + account.getServer() + "]";
    }
}
