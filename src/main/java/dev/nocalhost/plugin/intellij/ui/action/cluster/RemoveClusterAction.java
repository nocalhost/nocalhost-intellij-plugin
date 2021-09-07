package dev.nocalhost.plugin.intellij.ui.action.cluster;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;

import dev.nocalhost.plugin.intellij.nhctl.NhctlDeleteKubeConfigCommand;
import dev.nocalhost.plugin.intellij.settings.NocalhostSettings;
import dev.nocalhost.plugin.intellij.settings.data.StandaloneCluster;
import dev.nocalhost.plugin.intellij.topic.NocalhostTreeUpdateNotifier;
import dev.nocalhost.plugin.intellij.ui.tree.node.ClusterNode;
import dev.nocalhost.plugin.intellij.utils.KubeConfigUtil;
import lombok.SneakyThrows;

public class RemoveClusterAction extends DumbAwareAction {
    private final NocalhostSettings nocalhostSettings = ApplicationManager.getApplication().getService(
            NocalhostSettings.class);

    private final Project project;
    private final ClusterNode node;

    public RemoveClusterAction(Project project, ClusterNode node) {
        super("Remove", "", AllIcons.Vcs.Remove);
        this.project = project;
        this.node = node;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        ProgressManager.getInstance().run(new Task.Backgroundable(
                project,
                "Removing cluster"
        ) {

            @Override
            public void onSuccess() {
                super.onSuccess();
                ApplicationManager.getApplication().getMessageBus().syncPublisher(
                        NocalhostTreeUpdateNotifier.NOCALHOST_TREE_UPDATE_NOTIFIER_TOPIC).action();
            }

            @Override
            @SneakyThrows
            public void run(@NotNull ProgressIndicator indicator) {
                var cmd = new NhctlDeleteKubeConfigCommand();
                cmd.setKubeConfig(KubeConfigUtil.kubeConfigPath(node.getRawKubeConfig()));
                cmd.execute();

                nocalhostSettings.removeStandaloneCluster(
                        new StandaloneCluster(node.getRawKubeConfig()));
            }
        });
    }
}
