package dev.nocalhost.plugin.intellij.task;

import com.google.common.collect.Lists;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import dev.nocalhost.plugin.intellij.data.kubeconfig.KubeConfig;
import dev.nocalhost.plugin.intellij.data.kubeconfig.KubeContext;
import dev.nocalhost.plugin.intellij.exception.NocalhostNotifier;
import dev.nocalhost.plugin.intellij.nhctl.NhctlCreateKubeConfigCommand;
import dev.nocalhost.plugin.intellij.settings.NocalhostSettings;
import dev.nocalhost.plugin.intellij.settings.data.StandaloneCluster;
import dev.nocalhost.plugin.intellij.topic.NocalhostTreeUpdateNotifier;
import dev.nocalhost.plugin.intellij.utils.DataUtils;
import dev.nocalhost.plugin.intellij.utils.ErrorUtil;
import dev.nocalhost.plugin.intellij.utils.KubeConfigUtil;
import lombok.SneakyThrows;

public class AddStandaloneClusterTask extends Task.Backgroundable {
    private final NocalhostSettings nocalhostSettings = ApplicationManager.getApplication().getService(
            NocalhostSettings.class);

    private final String rawKubeConfig;
    private final KubeContext kubeContext;

    public AddStandaloneClusterTask(@Nullable Project project,
                                    String rawKubeConfig,
                                    KubeContext kubeContext) {
        super(project, "Adding standalone clusters");
        this.kubeContext = kubeContext;
        this.rawKubeConfig = rawKubeConfig;
    }

    @SneakyThrows
    @Override
    public void run(@NotNull ProgressIndicator indicator) {
        var srcKubeConfig = DataUtils.YAML.loadAs(rawKubeConfig, KubeConfig.class);
        var outKubeConfig = new KubeConfig();
        outKubeConfig.setCurrentContext(kubeContext.getName());

        srcKubeConfig
                .getContexts()
                .stream()
                .filter(x -> StringUtils.equals(x.getName(), kubeContext.getName()))
                .findFirst()
                .ifPresent(x -> outKubeConfig.setContexts(Lists.newArrayList(x)));

        srcKubeConfig
                .getClusters()
                .stream()
                .filter(x -> StringUtils.equals(x.getName(), kubeContext.getContext().getCluster()))
                .findFirst()
                .ifPresent(x -> outKubeConfig.setClusters(Lists.newArrayList(x)));

        srcKubeConfig
                .getUsers()
                .stream()
                .filter(x -> StringUtils.equals(x.getName(), kubeContext.getContext().getUser()))
                .findFirst()
                .ifPresent(x -> outKubeConfig.setUsers(Lists.newArrayList(x)));

        var kubeConfigText = DataUtils.toYaml(outKubeConfig);
        nocalhostSettings.updateStandaloneCluster(new StandaloneCluster(kubeConfigText));

        var cmd = new NhctlCreateKubeConfigCommand(getProject());
        cmd.setKubeConfig(KubeConfigUtil.kubeConfigPath(kubeConfigText));
        cmd.execute();
    }

    @Override
    public void onSuccess() {
        super.onSuccess();
        ApplicationManager.getApplication().getMessageBus().syncPublisher(
                NocalhostTreeUpdateNotifier.NOCALHOST_TREE_UPDATE_NOTIFIER_TOPIC).action();
        NocalhostNotifier.getInstance(getProject()).notifySuccess(
                "Adding standalone clusters success", "");
    }

    @Override
    public void onThrowable(@NotNull Throwable e) {
        ErrorUtil.dealWith(this.getProject(), "Adding standalone clusters error",
                "Error occurred while adding standalone cluster", e);
    }
}
