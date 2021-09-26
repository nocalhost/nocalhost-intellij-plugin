package dev.nocalhost.plugin.intellij.service;

import com.intellij.openapi.project.Project;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicReference;

import dev.nocalhost.plugin.intellij.commands.data.NhctlDevAssociateQueryResult;
import dev.nocalhost.plugin.intellij.nhctl.NhctlAssociateQueryerCommand;
import dev.nocalhost.plugin.intellij.data.NocalhostContext;
import dev.nocalhost.plugin.intellij.utils.DataUtils;
import dev.nocalhost.plugin.intellij.utils.ErrorUtil;

public class NocalhostContextManager {
    private final Project project;
    private final AtomicReference<NocalhostContext> ref = new AtomicReference<>(null);

    public NocalhostContextManager(Project project) {
        this.project = project;
    }

    public static NocalhostContextManager getInstance(@NotNull Project project) {
        return project.getService(NocalhostContextManager.class);
    }

    public @Nullable NocalhostContext getContext() {
        return ref.get();
    }

    public void refresh() {
        try {
            var path = project.getBasePath();
            if (StringUtils.isEmpty(path)) {
                return;
            }

            var cmd = new NhctlAssociateQueryerCommand();
            cmd.setCurrent(true);
            cmd.setAssociate(Paths.get(path).toString());

            var json = cmd.execute();
            if (StringUtils.isEmpty(json)) {
                ref.set(null);
                return;
            }

            replace(DataUtils.GSON.fromJson(json, NhctlDevAssociateQueryResult.class));
        } catch (Exception ex) {
            ref.set(null);
            ErrorUtil.dealWith(
                    project,
                    "Failed to refresh context",
                    "Error occurred while refresh context",
                    ex
            );
        }
    }

    public void replace(@Nullable NhctlDevAssociateQueryResult result) {
        var context = result == null ? null : NocalhostContext.builder()
                                     .kubeConfigPath(Paths.get(result.getKubeconfigPath()))
                                     .namespace(result.getServicePack().getNamespace())
                                     .applicationName(result.getServicePack().getApplicationName())
                                     .serviceType(result.getServicePack().getServiceType())
                                     .serviceName(result.getServicePack().getServiceName())
                                     .containerName(result.getServicePack().getContainer())
                                     .sha(result.getSha())
                                     .build();
        ref.set(context);
    }
}
