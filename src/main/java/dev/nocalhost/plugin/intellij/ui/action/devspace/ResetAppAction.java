package dev.nocalhost.plugin.intellij.ui.action.devspace;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageDialogBuilder;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import dev.nocalhost.plugin.intellij.NocalhostNotifier;
import dev.nocalhost.plugin.intellij.api.NocalhostApi;
import dev.nocalhost.plugin.intellij.api.data.DevSpace;
import dev.nocalhost.plugin.intellij.commands.OutputCapturedNhctlCommand;
import dev.nocalhost.plugin.intellij.commands.data.NhctlUninstallOptions;
import dev.nocalhost.plugin.intellij.topic.DevSpaceListUpdatedNotifier;
import dev.nocalhost.plugin.intellij.ui.tree.node.DevSpaceNode;
import dev.nocalhost.plugin.intellij.utils.KubeConfigUtil;

public class ResetAppAction extends AnAction {
    private static final Logger LOG = Logger.getInstance(ResetAppAction.class);

    private final Project project;
    private final DevSpaceNode node;

    public ResetAppAction(Project project, DevSpaceNode node) {
        super("Reset", "", AllIcons.General.Reset);
        this.project = project;
        this.node = node;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        final DevSpace devSpace = node.getDevSpace();

        final String appName = devSpace.getContext().getApplicationName();
        if (!MessageDialogBuilder.yesNo("Reset application", "Reset application " + appName + "?").guessWindowAndAsk()) {
            return;
        }

        ProgressManager.getInstance().run(new Task.Backgroundable(null, "Reset application: " + appName, false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                final OutputCapturedNhctlCommand outputCapturedNhctlCommand = project.getService(OutputCapturedNhctlCommand.class);

                NhctlUninstallOptions opts = new NhctlUninstallOptions();
                opts.setForce(true);
                opts.setKubeconfig(KubeConfigUtil.kubeConfigPath(devSpace).toString());
                try {
                    try {
                        outputCapturedNhctlCommand.uninstall(appName, opts);
                    } catch (Exception e) {
//                        LOG.error("error occurred while uninstall application", e);
                    }

                    final NocalhostApi nocalhostApi = ServiceManager.getService(NocalhostApi.class);
                    nocalhostApi.recreate(devSpace);

                    final Application application = ApplicationManager.getApplication();
                    DevSpaceListUpdatedNotifier publisher = application.getMessageBus()
                            .syncPublisher(DevSpaceListUpdatedNotifier.DEV_SPACE_LIST_UPDATED_NOTIFIER_TOPIC);
                    publisher.action();

                    NocalhostNotifier.getInstance(project).notifySuccess("Application " + appName + " reset complete", "");

                } catch (IOException e) {
                    LOG.error("error occurred while reset application", e);
                }
            }
        });
    }
}
