package dev.nocalhost.plugin.intellij.ui.action.application;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageDialogBuilder;

import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

import dev.nocalhost.plugin.intellij.commands.OutputCapturedNhctlCommand;
import dev.nocalhost.plugin.intellij.commands.data.NhctlUninstallOptions;
import dev.nocalhost.plugin.intellij.exception.NocalhostNotifier;
import dev.nocalhost.plugin.intellij.i18n.NocalhostI18n;
import dev.nocalhost.plugin.intellij.task.BaseBackgroundTask;
import dev.nocalhost.plugin.intellij.topic.NocalhostTreeUpdateNotifier;
import dev.nocalhost.plugin.intellij.ui.tree.node.ApplicationNode;
import dev.nocalhost.plugin.intellij.utils.ErrorUtil;
import dev.nocalhost.plugin.intellij.utils.KubeConfigUtil;
import lombok.SneakyThrows;

public class UninstallAppAction extends DumbAwareAction {
    private final OutputCapturedNhctlCommand outputCapturedNhctlCommand;

    private final Project project;
    private final Path kubeConfigPath;
    private final String namespace;
    private final String applicationName;

    public UninstallAppAction(Project project, ApplicationNode node) {
        super(NocalhostI18n.get("action.uninstallApp"), "", AllIcons.Actions.Uninstall);
        this.project = project;
        this.kubeConfigPath = KubeConfigUtil.toPath(node.getClusterNode().getRawKubeConfig());
        this.namespace = node.getNamespaceNode().getNamespace();
        this.applicationName = node.getName();
        outputCapturedNhctlCommand = project.getService(OutputCapturedNhctlCommand.class);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        if (!MessageDialogBuilder.yesNo(NocalhostI18n.get("action.uninstallApp"), NocalhostI18n.format("confirm.uninstallApp", applicationName)).ask(project)) {
            return;
        }
        ProgressManager.getInstance().run(new BaseBackgroundTask(
                null,
                NocalhostI18n.format("progress.uninstallApp", applicationName)
        ) {
            @Override
            public void onSuccess() {
                super.onSuccess();
                ApplicationManager.getApplication().getMessageBus().syncPublisher(
                        NocalhostTreeUpdateNotifier.NOCALHOST_TREE_UPDATE_NOTIFIER_TOPIC).action();

                NocalhostNotifier.getInstance(project).notifySuccess(
                        NocalhostI18n.format("success.uninstallApp", applicationName),
                        "");
            }

            @Override
            public void onThrowable(@NotNull Throwable e) {
                ErrorUtil.dealWith(project, NocalhostI18n.get("error.uninstallApplication"),
                        NocalhostI18n.get("error.uninstallApplication.content"), e);
            }

            @SneakyThrows
            @Override
            public void runTask(@NotNull ProgressIndicator indicator) {
                NhctlUninstallOptions opts = new NhctlUninstallOptions(kubeConfigPath, namespace, this);
                opts.setForce(true);
                outputCapturedNhctlCommand.uninstall(applicationName, opts);
            }
        });
    }
}
