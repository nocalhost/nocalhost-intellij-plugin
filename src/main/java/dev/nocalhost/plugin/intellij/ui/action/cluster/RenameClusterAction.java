package dev.nocalhost.plugin.intellij.ui.action.cluster;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import dev.nocalhost.plugin.intellij.config.NocalhostConfig;
import dev.nocalhost.plugin.intellij.data.kubeconfig.KubeConfig;
import dev.nocalhost.plugin.intellij.settings.data.StandaloneCluster;
import dev.nocalhost.plugin.intellij.topic.NocalhostTreeUpdateNotifier;
import dev.nocalhost.plugin.intellij.ui.tree.node.ClusterNode;
import dev.nocalhost.plugin.intellij.utils.DataUtils;
import dev.nocalhost.plugin.intellij.utils.ErrorUtil;
import lombok.SneakyThrows;

public class RenameClusterAction extends DumbAwareAction {
    private final NocalhostConfig nocalhostConfig = ApplicationManager.getApplication()
            .getService(NocalhostConfig.class);

    private final Project project;
    private final ClusterNode node;

    public RenameClusterAction(Project project, ClusterNode node) {
        super("Rename", "", AllIcons.Actions.Edit);
        this.project = project;
        this.node = node;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        String name = Messages.showInputDialog(project, "Input cluster name", "Rename Standalone Cluster", null);
        if (!StringUtils.isNotEmpty(name)) {
            return;
        }

        ProgressManager.getInstance().run(new Task.Backgroundable(
                project,
                "Renaming cluster"
        ) {

            @Override
            public void onSuccess() {
                ApplicationManager.getApplication().getMessageBus().syncPublisher(
                        NocalhostTreeUpdateNotifier.NOCALHOST_TREE_UPDATE_NOTIFIER_TOPIC).action();
            }

            @Override
            public void onThrowable(@NotNull Throwable e) {
                ErrorUtil.dealWith(project, "Renaming cluster error",
                        "Error occurs while renaming cluster", e);
            }

            @SneakyThrows
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                String oldRawKubeConfig = node.getRawKubeConfig();
                KubeConfig kubeConfig = DataUtils.fromYaml(oldRawKubeConfig, KubeConfig.class);
                kubeConfig.getClusters().get(0).setName(name);
                kubeConfig.getContexts().get(0).getContext().setCluster(name);
                String newRawKubeConfig = DataUtils.toYaml(kubeConfig);

                node.updateFrom(new ClusterNode(newRawKubeConfig, kubeConfig));

                nocalhostConfig.removeStandaloneCluster(new StandaloneCluster(oldRawKubeConfig));
                nocalhostConfig.updateStandaloneCluster(new StandaloneCluster(newRawKubeConfig));
            }
        });
    }
}
