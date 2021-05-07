package dev.nocalhost.plugin.intellij.task;

import com.google.common.collect.Lists;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.MessageFormat;
import java.util.List;
import java.util.Optional;

import dev.nocalhost.plugin.intellij.commands.data.KubeCluster;
import dev.nocalhost.plugin.intellij.commands.data.KubeConfig;
import dev.nocalhost.plugin.intellij.commands.data.KubeContext;
import dev.nocalhost.plugin.intellij.commands.data.KubeUser;
import dev.nocalhost.plugin.intellij.exception.NocalhostNotifier;
import dev.nocalhost.plugin.intellij.settings.NocalhostSettings;
import dev.nocalhost.plugin.intellij.settings.data.StandaloneCluster;
import dev.nocalhost.plugin.intellij.topic.NocalhostTreeUpdateNotifier;
import dev.nocalhost.plugin.intellij.utils.DataUtils;
import lombok.SneakyThrows;

public class AddStandaloneClusterTask extends Task.Backgroundable {
    private final NocalhostSettings nocalhostSettings = ServiceManager.getService(
            NocalhostSettings.class);

    private final String rawKubeConfig;
    private final List<KubeContext> kubeContexts;

    public AddStandaloneClusterTask(@Nullable Project project,
                                    String rawKubeConfig,
                                    List<KubeContext> kubeContexts) {
        super(project, "Adding standalone clusters");
        this.rawKubeConfig = rawKubeConfig;
        this.kubeContexts = kubeContexts;
    }

    @SneakyThrows
    @Override
    public void run(@NotNull ProgressIndicator indicator) {
        KubeConfig srcKubeConfig = DataUtils.YAML.loadAs(rawKubeConfig, KubeConfig.class);
        for (KubeContext kubeContext : kubeContexts) {
            KubeConfig outKubeConfig = new KubeConfig();
            outKubeConfig.setCurrentContext(kubeContext.getName());
            outKubeConfig.setContexts(Lists.newArrayList(kubeContext));

            Optional<KubeCluster> kubeClusterOptional = srcKubeConfig.getClusters().stream()
                    .filter(e -> StringUtils.equals(
                            e.getName(),
                            kubeContext.getContext().getCluster())
                    )
                    .findFirst();
            if (kubeClusterOptional.isEmpty()) {
                throw new IllegalArgumentException(MessageFormat.format(
                        "No cluster found for context {0}",
                        DataUtils.GSON.toJson(kubeContext)));
            }
            outKubeConfig.setClusters(Lists.newArrayList(kubeClusterOptional.get()));

            Optional<KubeUser> kubeUserOptional = srcKubeConfig.getUsers().stream()
                    .filter(e -> StringUtils.equals(
                            e.getName(),
                            kubeContext.getContext().getUser())
                    )
                    .findFirst();
            if (kubeUserOptional.isEmpty()) {
                throw new IllegalArgumentException(MessageFormat.format(
                        "No user found for context {0}",
                        DataUtils.GSON.toJson(kubeContext)));
            }
            outKubeConfig.setUsers(Lists.newArrayList(kubeUserOptional.get()));

            String kubeConfigText = DataUtils.toYaml(outKubeConfig);
            nocalhostSettings.updateStandaloneCluster(new StandaloneCluster(kubeConfigText));
        }
    }

    @Override
    public void onSuccess() {
        ApplicationManager.getApplication().getMessageBus().syncPublisher(
                NocalhostTreeUpdateNotifier.NOCALHOST_TREE_UPDATE_NOTIFIER_TOPIC).action();
        NocalhostNotifier.getInstance(getProject()).notifySuccess(
                "Adding standalone clusters success", "");
    }

    @Override
    public void onThrowable(@NotNull Throwable e) {
        NocalhostNotifier.getInstance(getProject()).notifyError(
                "Adding standalone clusters error",
                "Error occurred while adding standalone cluster",
                e.getMessage()
        );
    }
}
