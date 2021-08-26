package dev.nocalhost.plugin.intellij.service;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;

import org.apache.commons.lang3.StringUtils;

import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicReference;

import dev.nocalhost.plugin.intellij.commands.NhctlCommand;
import dev.nocalhost.plugin.intellij.commands.data.NhctlDevAssociateQueryResult;
import dev.nocalhost.plugin.intellij.commands.data.NhctlDevAssociateQueryerOptions;
import dev.nocalhost.plugin.intellij.data.ServiceProjectPath;
import dev.nocalhost.plugin.intellij.exception.NocalhostExecuteCmdException;
import dev.nocalhost.plugin.intellij.utils.DataUtils;
import dev.nocalhost.plugin.intellij.utils.ErrorUtil;

public class NocalhostProjectService {
    private final AtomicReference<ServiceProjectPath> serviceProjectPathAtomicReference = new AtomicReference<>(null);
    private final Project project;

    public NocalhostProjectService(Project project) {
        this.project = project;
    }

    public void refreshServiceProjectPath() {
        final String projectPath = project.getBasePath();
        final NhctlCommand nhctlCommand = ApplicationManager.getApplication()
                .getService(NhctlCommand.class);
        final NhctlDevAssociateQueryerOptions opts = new NhctlDevAssociateQueryerOptions();
        opts.setAssociate(Paths.get(projectPath).toString());
        opts.setCurrent(true);
        try {
            String output = nhctlCommand.devAssociateQueryer(opts);
            if (!StringUtils.isNotEmpty(output)) {
                serviceProjectPathAtomicReference.set(null);
                return;
            }
            NhctlDevAssociateQueryResult result = DataUtils.GSON
                    .fromJson(output, NhctlDevAssociateQueryResult.class);
            ServiceProjectPath serviceProjectPath = ServiceProjectPath.builder()
                    .kubeConfigPath(Paths.get(result.getKubeconfigPath()))
                    .namespace(result.getServicePack().getNamespace())
                    .applicationName(result.getServicePack().getApplicationName())
                    .serviceType(result.getServicePack().getServiceType())
                    .serviceName(result.getServicePack().getServiceName())
                    .containerName(result.getServicePack().getContainer())
                    .build();
            serviceProjectPathAtomicReference.set(serviceProjectPath);
        } catch (NocalhostExecuteCmdException e) {
            ErrorUtil.dealWith(project, "Query associate directory error",
                    "Error occurs while querying associate directory", e);
            serviceProjectPathAtomicReference.set(null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public ServiceProjectPath getServiceProjectPath() {
        return serviceProjectPathAtomicReference.get();
    }
}
