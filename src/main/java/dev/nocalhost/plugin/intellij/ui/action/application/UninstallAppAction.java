package dev.nocalhost.plugin.intellij.ui.action.application;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageDialogBuilder;
import com.intellij.openapi.ui.Messages;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import dev.nocalhost.plugin.intellij.api.data.Application;
import dev.nocalhost.plugin.intellij.api.data.DevSpace;
import dev.nocalhost.plugin.intellij.commands.OutputCapturedNhctlCommand;
import dev.nocalhost.plugin.intellij.commands.data.NhctlUninstallOptions;
import dev.nocalhost.plugin.intellij.exception.NocalhostExecuteCmdException;
import dev.nocalhost.plugin.intellij.exception.NocalhostNotifier;
import dev.nocalhost.plugin.intellij.helpers.NhctlHelper;
import dev.nocalhost.plugin.intellij.topic.NocalhostTreeDataUpdateNotifier;
import dev.nocalhost.plugin.intellij.ui.tree.node.ApplicationNode;

public class UninstallAppAction extends AnAction {
    private static final Logger LOG = Logger.getInstance(UninstallAppAction.class);

    private final Project project;
    private final ApplicationNode node;

    public UninstallAppAction(Project project, ApplicationNode node) {
        super("Uninstall App", "", AllIcons.Actions.Uninstall);
        this.project = project;
        this.node = node;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        final Application application = node.getApplication();
        final DevSpace devSpace = node.getDevSpace();

        try {
            if (!NhctlHelper.isApplicationInstalled(devSpace, application)) {
                Messages.showMessageDialog("Application has not been installed.", "Uninstall application", null);
                return;
            }
        } catch (IOException | InterruptedException e) {
            LOG.error("error occurred while checking if application was installed", e);
            return;
        }

        final String appName = application.getContext().getApplicationName();
        if (!MessageDialogBuilder.yesNo("Uninstall application", "Uninstall application " + appName + "?").guessWindowAndAsk()) {
            return;
        }

        ProgressManager.getInstance().run(new Task.Backgroundable(null, "Uninstalling application: " + appName, false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                final OutputCapturedNhctlCommand outputCapturedNhctlCommand = project.getService(OutputCapturedNhctlCommand.class);

                NhctlUninstallOptions opts = new NhctlUninstallOptions(devSpace);
                opts.setForce(true);
                try {
                    outputCapturedNhctlCommand.uninstall(appName, opts);

                    ApplicationManager.getApplication().getMessageBus().syncPublisher(
                            NocalhostTreeDataUpdateNotifier
                                    .NOCALHOST_TREE_DATA_UPDATE_NOTIFIER_TOPIC
                    ).action();

                    NocalhostNotifier.getInstance(project).notifySuccess("Application " + appName + " uninstalled", "");

                } catch (InterruptedException | IOException | NocalhostExecuteCmdException e) {
                    LOG.error("error occurred while uninstalling application", e);
                }
            }
        });
    }
}
