package dev.nocalhost.plugin.intellij.ui.tree.listerner.workload;

import com.intellij.ide.impl.OpenProjectTask;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.ex.ProjectManagerEx;
import com.intellij.openapi.ui.MessageDialogBuilder;
import com.intellij.openapi.ui.Messages;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.nio.file.Path;

import dev.nocalhost.plugin.intellij.api.data.DevModeService;
import dev.nocalhost.plugin.intellij.settings.NocalhostSettings;
import dev.nocalhost.plugin.intellij.ui.tree.node.ResourceNode;

public class StartDevelop implements ActionListener {

    private final ResourceNode node;

    public StartDevelop(ResourceNode node) {
        this.node = node;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        int exitCode = MessageDialogBuilder.yesNoCancel("To start develop, you must specify source code directory.", "")
                .yesText("Clone from Git Repo")
                .noText("Open local directly")
                .guessWindowAndAsk();
        switch (exitCode) {
            case Messages.YES:
                // TODO: Git.getInstance().clone(...)
                break;
            case Messages.NO:
                final FileChooserDescriptor dirChooser = FileChooserDescriptorFactory.createSingleFolderDescriptor();
                dirChooser.setShowFileSystemRoots(true);
                FileChooser.chooseFiles(dirChooser, null, null, paths -> {
                    Path bashPath = paths.get(0).toNioPath();

                    final NocalhostSettings nocalhostSettings = ServiceManager.getService(NocalhostSettings.class);

                    nocalhostSettings.getDevModeProjectBasePath2Service().put(
                            bashPath.toString(),
                            new DevModeService(node.devSpace().getId(), node.devSpace().getDevSpaceId(), node.resourceName())
                    );

                    ProjectManagerEx.getInstanceEx().openProject(bashPath, new OpenProjectTask());
                });
                break;
            default:
        }
    }
}
