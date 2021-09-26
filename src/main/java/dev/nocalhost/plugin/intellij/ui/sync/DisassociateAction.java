package dev.nocalhost.plugin.intellij.ui.sync;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Paths;

import dev.nocalhost.plugin.intellij.commands.data.NhctlDevAssociateQueryResult;
import dev.nocalhost.plugin.intellij.data.NocalhostContext;
import dev.nocalhost.plugin.intellij.nhctl.NhctlDevAssociateCommand;
import dev.nocalhost.plugin.intellij.service.NocalhostContextManager;
import dev.nocalhost.plugin.intellij.utils.ErrorUtil;

public class DisassociateAction extends DumbAwareAction {
    private final Project project;
    private final NhctlDevAssociateQueryResult result;

    public DisassociateAction(@NotNull Project project, @NotNull NhctlDevAssociateQueryResult result) {
        super("Disassociate from Current Directory");
        this.result = result;
        this.project = project;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        var basePath = project.getBasePath();
        if (basePath != null) {
            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                try {
                    var cmd = new NhctlDevAssociateCommand();
                    cmd.setDeAssociate(true);
                    cmd.setAssociate(Paths.get(basePath).toString());
                    cmd.setKubeConfig(Paths.get(result.getKubeconfigPath()));
                    cmd.setNamespace(result.getServicePack().getNamespace());
                    cmd.setContainer(result.getServicePack().getContainer());
                    cmd.setDeployment(result.getServicePack().getServiceName());
                    cmd.setControllerType(result.getServicePack().getServiceType());
                    cmd.setApplicationName(result.getServicePack().getApplicationName());
                    cmd.execute();

                    // if disassociate current service
                    var manager = NocalhostContextManager.getInstance(project);
                    var context = manager.getContext();
                    if (context != null && StringUtils.equals(context.getSha(), result.getSha())) {
                        manager.replace(null);
                    }
                } catch (Exception ex) {
                    ErrorUtil.dealWith(project, "Disassociate from Current Directory", "Error occurred while disassociate from current directory.", ex);
                }
            });
        }
    }
}
