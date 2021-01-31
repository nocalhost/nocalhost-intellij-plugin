package dev.nocalhost.plugin.intellij.ui.tree.listerner.devspace;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import dev.nocalhost.plugin.intellij.api.data.DevSpace;
import dev.nocalhost.plugin.intellij.helpers.NhctlHelper;
import dev.nocalhost.plugin.intellij.ui.InstallDevSpaceDialog;
import dev.nocalhost.plugin.intellij.ui.tree.node.DevSpaceNode;

public class Install implements ActionListener {
    private static final Logger LOG = Logger.getInstance(Install.class);

    private final Project project;
    private final DevSpaceNode node;

    public Install(Project project, DevSpaceNode node) {
        this.project = project;
        this.node = node;
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        final DevSpace devSpace = node.getDevSpace();

        try {
            if (NhctlHelper.isApplicationInstalled(devSpace)) {
                Messages.showMessageDialog("Application has been installed.", "Install application", null);
                return;
            }
        } catch (IOException | InterruptedException e) {
            LOG.error("error occurred while checking if application was installed", e);
            return;
        }

        new InstallDevSpaceDialog(project, node.getDevSpace()).showAndGet();
    }
}
