package dev.nocalhost.plugin.intellij.ui.action.namespace;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageDialogBuilder;

import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

import dev.nocalhost.plugin.intellij.api.NocalhostApi;
import dev.nocalhost.plugin.intellij.commands.OutputCapturedNhctlCommand;
import dev.nocalhost.plugin.intellij.commands.data.NhctlResetDevSpaceOptions;
import dev.nocalhost.plugin.intellij.exception.NocalhostNotifier;
import dev.nocalhost.plugin.intellij.topic.NocalhostTreeUpdateNotifier;
import dev.nocalhost.plugin.intellij.ui.tree.node.NamespaceNode;
import dev.nocalhost.plugin.intellij.utils.ErrorUtil;
import dev.nocalhost.plugin.intellij.utils.KubeConfigUtil;
import lombok.SneakyThrows;

public class ResetDevSpaceAction extends DumbAwareAction {
    private final NocalhostApi nocalhostApi = ServiceManager.getService(NocalhostApi.class);

    private final OutputCapturedNhctlCommand outputCapturedNhctlCommand;

    private final Project project;
    private final NamespaceNode node;
    private final Path kubeConfigPath;
    private final String namespace;

    public ResetDevSpaceAction(Project project, NamespaceNode node) {
        super("Reset", "", AllIcons.General.Reset);
        this.project = project;
        this.node = node;
        this.kubeConfigPath = KubeConfigUtil.kubeConfigPath(node.getClusterNode().getRawKubeConfig());
        this.namespace = node.getName();
        outputCapturedNhctlCommand = project.getService(OutputCapturedNhctlCommand.class);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        final String name = StringUtils.isNotEmpty(node.getSpaceName()) ? node.getSpaceName() : node.getName();

        if (!MessageDialogBuilder.yesNo("Reset DevSpace", "Reset " + name + "?").guessWindowAndAsk()) {
            return;
        }

        ProgressManager.getInstance().run(new Task.Backgroundable(null, "Reset DevSpace: " + name, false) {
            @Override
            public void onSuccess() {
                ApplicationManager.getApplication().getMessageBus().syncPublisher(
                        NocalhostTreeUpdateNotifier.NOCALHOST_TREE_UPDATE_NOTIFIER_TOPIC).action();
                NocalhostNotifier.getInstance(project).notifySuccess(
                        "DevSpace " + name + " reset complete", "");
            }

            @Override
            public void onThrowable(@NotNull Throwable e) {
                ErrorUtil.dealWith(project, "Resetting dev space error",
                        "Error occurs while resetting dev space", e);
            }

            @SneakyThrows
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                NhctlResetDevSpaceOptions opts = new NhctlResetDevSpaceOptions(kubeConfigPath, namespace);
                outputCapturedNhctlCommand.resetDevSpace(opts);

                nocalhostApi.recreate(
                        node.getClusterNode().getNocalhostAccount().getServer(),
                        node.getClusterNode().getNocalhostAccount().getJwt(),
                        node.getSpaceId());
            }
        });
    }
}
