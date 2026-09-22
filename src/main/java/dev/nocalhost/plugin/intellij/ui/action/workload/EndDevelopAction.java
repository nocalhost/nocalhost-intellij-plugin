package dev.nocalhost.plugin.intellij.ui.action.workload;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageDialogBuilder;

import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

import dev.nocalhost.plugin.intellij.commands.NhctlCommand;
import dev.nocalhost.plugin.intellij.commands.OutputCapturedNhctlCommand;
import dev.nocalhost.plugin.intellij.commands.data.NhctlDevEndOptions;
import dev.nocalhost.plugin.intellij.exception.NocalhostNotifier;
import dev.nocalhost.plugin.intellij.i18n.NocalhostI18n;
import dev.nocalhost.plugin.intellij.task.BaseBackgroundTask;
import dev.nocalhost.plugin.intellij.topic.NocalhostTreeUpdateNotifier;
import dev.nocalhost.plugin.intellij.ui.tree.node.ResourceNode;
import dev.nocalhost.plugin.intellij.service.NocalhostDebugReconnectService;
import dev.nocalhost.plugin.intellij.utils.ErrorUtil;
import dev.nocalhost.plugin.intellij.utils.KubeConfigUtil;
import dev.nocalhost.plugin.intellij.utils.NhctlDescribeServiceUtil;
import dev.nocalhost.plugin.intellij.utils.NhctlUtil;
import icons.NocalhostIcons;
import lombok.SneakyThrows;

public class EndDevelopAction extends DumbAwareAction {
    private final OutputCapturedNhctlCommand outputCapturedNhctlCommand;

    private final Project project;
    private final ResourceNode node;
    private final Path kubeConfigPath;
    private final String namespace;

    public EndDevelopAction(Project project, ResourceNode node) {
        super(NocalhostI18n.get("action.endDevMode"), "", NocalhostIcons.Status.DevEnd);
        this.project = project;
        this.node = node;
        this.kubeConfigPath = KubeConfigUtil.toPath(node.getClusterNode().getRawKubeConfig());
        this.namespace = node.getNamespaceNode().getNamespace();
        outputCapturedNhctlCommand = project.getService(OutputCapturedNhctlCommand.class);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                var desService = NhctlUtil.getDescribeService(
                        project, node.resourceName(), node.controllerType(),
                        namespace, node.applicationName(), kubeConfigPath
                );
                ApplicationManager.getApplication().invokeLater(() -> {
                    if (NhctlDescribeServiceUtil.developStarting(desService)) {
                        endDevelop();
                        return;
                    }

                    if (desService.isPossess()) {
                        endDevelop();
                    } else {
                        if (MessageDialogBuilder.yesNo(
                                NocalhostI18n.get("action.endDevMode"),
                                NocalhostI18n.get("confirm.endDevMode")
                        ).ask(project)) {
                            endDevelop();
                        }
                    }
                });
            } catch (Exception ex) {
                ErrorUtil.dealWith(project, NocalhostI18n.get("error.failedLoadServiceStatus"),
                        NocalhostI18n.get("error.failedLoadServiceStatus.content"), ex);
            }
        });
    }

    private void endDevelop() {
        NocalhostDebugReconnectService.getInstance(project).suppressAutoReconnect("end dev mode", 120_000L);
        // 退出 DevMode 时远端 JVM 会消失（且耗时可能较长），这段时间不要自动重连
        ProgressManager.getInstance().run(new BaseBackgroundTask(null, NocalhostI18n.get("progress.endDevMode")) {
            @Override
            public void onSuccess() {
                super.onSuccess();
                ApplicationManager.getApplication().getMessageBus().syncPublisher(
                        NocalhostTreeUpdateNotifier.NOCALHOST_TREE_UPDATE_NOTIFIER_TOPIC).action();

                NocalhostNotifier.getInstance(project).notifySuccess(NocalhostI18n.get("success.endDevMode"), "");
            }

            @Override
            public void onThrowable(@NotNull Throwable e) {
                ErrorUtil.dealWith(project, NocalhostI18n.get("error.endDevMode"),
                        NocalhostI18n.get("error.endDevMode.content"), e);
            }

            @SneakyThrows
            @Override
            public void runTask(@NotNull ProgressIndicator indicator) {
                NhctlDevEndOptions opts = new NhctlDevEndOptions(kubeConfigPath, namespace, this);
                opts.setDeployment(node.resourceName());
                opts.setControllerType(node.controllerType());

                outputCapturedNhctlCommand.devEnd(node.applicationName(), opts);
            }
        });
    }
}
