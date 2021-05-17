package dev.nocalhost.plugin.intellij.ui.action.application;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageDialogBuilder;
import com.intellij.openapi.ui.Messages;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Path;

import dev.nocalhost.plugin.intellij.commands.OutputCapturedNhctlCommand;
import dev.nocalhost.plugin.intellij.commands.data.NhctlUninstallOptions;
import dev.nocalhost.plugin.intellij.exception.NocalhostNotifier;
import dev.nocalhost.plugin.intellij.helpers.NhctlHelper;
import dev.nocalhost.plugin.intellij.topic.NocalhostTreeUpdateNotifier;
import dev.nocalhost.plugin.intellij.ui.tree.node.ApplicationNode;
import dev.nocalhost.plugin.intellij.utils.KubeConfigUtil;
import lombok.SneakyThrows;

public class UninstallAppAction extends DumbAwareAction {
    private static final Logger LOG = Logger.getInstance(UninstallAppAction.class);

    final OutputCapturedNhctlCommand outputCapturedNhctlCommand;

    private final Project project;
    private final Path kubeConfigPath;
    private final String namespace;
    private final String applicationName;

    public UninstallAppAction(Project project, ApplicationNode node) {
        super("Uninstall Application", "", AllIcons.Actions.Uninstall);
        this.project = project;
        this.kubeConfigPath = KubeConfigUtil.kubeConfigPath(node.getClusterNode().getRawKubeConfig());
        this.namespace = node.getNamespaceNode().getName();
        this.applicationName = node.getName();
        outputCapturedNhctlCommand = project.getService(OutputCapturedNhctlCommand.class);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                if (!NhctlHelper.isApplicationInstalled(kubeConfigPath, namespace, applicationName)) {
                    ApplicationManager.getApplication().invokeLater(() -> {
                        Messages.showMessageDialog(
                                "Application has not been installed.",
                                "Uninstall application",
                                null);
                    });

                    return;
                }

                ApplicationManager.getApplication().invokeLater(() -> {
                    if (!MessageDialogBuilder.yesNo(
                            "Uninstall application",
                            "Uninstall application " + applicationName + "?"
                    ).guessWindowAndAsk()) {
                        return;
                    }

                    uninstall();
                });
            } catch (IOException | InterruptedException e) {
                LOG.error("error occurred while checking if application was installed", e);
                return;
            }
        });
    }

    private void uninstall() {
        ProgressManager.getInstance().run(new Task.Backgroundable(
                null,
                "Uninstalling application: " + applicationName,
                false
        ) {
            @Override
            public void onSuccess() {
                ApplicationManager.getApplication().getMessageBus().syncPublisher(
                        NocalhostTreeUpdateNotifier.NOCALHOST_TREE_UPDATE_NOTIFIER_TOPIC).action();

                NocalhostNotifier.getInstance(project).notifySuccess(
                        "Application " + applicationName + " uninstalled",
                        "");
            }

            @Override
            public void onThrowable(@NotNull Throwable e) {
                LOG.error("error occurred while uninstalling application", e);
            }

            @SneakyThrows
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                NhctlUninstallOptions opts = new NhctlUninstallOptions(kubeConfigPath, namespace);
                opts.setForce(true);
                outputCapturedNhctlCommand.uninstall(applicationName, opts);
            }
        });
    }
}
