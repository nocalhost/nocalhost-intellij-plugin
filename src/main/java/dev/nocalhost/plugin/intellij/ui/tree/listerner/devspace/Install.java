package dev.nocalhost.plugin.intellij.ui.tree.listerner.devspace;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import dev.nocalhost.plugin.intellij.api.data.DevSpace;
import dev.nocalhost.plugin.intellij.helpers.NhctlHelper;
import dev.nocalhost.plugin.intellij.ui.InstallDevSpaceDialog;
import dev.nocalhost.plugin.intellij.ui.tree.node.DevSpaceNode;
import dev.nocalhost.plugin.intellij.utils.MessageUtil;

public class Install implements ActionListener {

    private final DevSpaceNode node;

    public Install(DevSpaceNode node) {
        this.node = node;
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        final DevSpace devSpace = node.getDevSpace();

        try {
            if (NhctlHelper.isApplicationInstalled(devSpace)) {
                MessageUtil.showMessageDialog("Application has been installed.");
                return;
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return;
        }

        new InstallDevSpaceDialog(node.getDevSpace()).showAndGet();
    }
}
