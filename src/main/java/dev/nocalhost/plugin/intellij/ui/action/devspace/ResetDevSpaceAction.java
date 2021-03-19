package dev.nocalhost.plugin.intellij.ui.action.devspace;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageDialogBuilder;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import dev.nocalhost.plugin.intellij.api.NocalhostApi;
import dev.nocalhost.plugin.intellij.api.data.DevSpace;
import dev.nocalhost.plugin.intellij.commands.OutputCapturedNhctlCommand;
import dev.nocalhost.plugin.intellij.commands.data.NhctlResetDevSpaceOptions;
import dev.nocalhost.plugin.intellij.exception.NocalhostApiException;
import dev.nocalhost.plugin.intellij.exception.NocalhostExecuteCmdException;
import dev.nocalhost.plugin.intellij.exception.NocalhostNotifier;
import dev.nocalhost.plugin.intellij.topic.DevSpaceListUpdatedNotifier;
import dev.nocalhost.plugin.intellij.ui.tree.node.DevSpaceNode;
import dev.nocalhost.plugin.intellij.utils.KubeConfigUtil;

public class ResetDevSpaceAction extends AnAction {
    private static final Logger LOG = Logger.getInstance(ResetDevSpaceAction.class);

    private final Project project;
    private final DevSpaceNode node;

    public ResetDevSpaceAction(Project project, DevSpaceNode node) {
        super("Reset", "", AllIcons.General.Reset);
        this.project = project;
        this.node = node;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        final DevSpace devSpace = node.getDevSpace();

        final String name = devSpace.getSpaceName();
        if (!MessageDialogBuilder.yesNo("Reset DevSpace", "Reset " + name + "?").guessWindowAndAsk()) {
            return;
        }

        ProgressManager.getInstance().run(new Task.Backgroundable(null, "Reset DevSpace: " + name, false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                try {
                    final OutputCapturedNhctlCommand outputCapturedNhctlCommand = project.getService(OutputCapturedNhctlCommand.class);
                    NhctlResetDevSpaceOptions options = new NhctlResetDevSpaceOptions();
                    options.setKubeconfig(KubeConfigUtil.kubeConfigPath(node.getDevSpace()).toString());

                    outputCapturedNhctlCommand.resetDevSpace(options);

                    final NocalhostApi nocalhostApi = ServiceManager.getService(NocalhostApi.class);
                    nocalhostApi.recreate(devSpace);

                    DevSpaceListUpdatedNotifier publisher = ApplicationManager.getApplication().getMessageBus()
                            .syncPublisher(DevSpaceListUpdatedNotifier.DEV_SPACE_LIST_UPDATED_NOTIFIER_TOPIC);
                    publisher.action();

                    NocalhostNotifier.getInstance(project).notifySuccess("DevSpace " + name + " reset complete", "");

                } catch (IOException | InterruptedException | NocalhostExecuteCmdException | NocalhostApiException e) {
                    LOG.error("error occurred while reset DevSpace", e);
                }
            }
        });
    }
}
