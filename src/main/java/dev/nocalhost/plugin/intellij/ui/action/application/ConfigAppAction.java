package dev.nocalhost.plugin.intellij.ui.action.application;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

import org.jetbrains.annotations.NotNull;

import dev.nocalhost.plugin.intellij.commands.NhctlCommand;
import dev.nocalhost.plugin.intellij.commands.data.NhctlConfigOptions;
import dev.nocalhost.plugin.intellij.ui.tree.node.ApplicationNode;
import dev.nocalhost.plugin.intellij.ui.vfs.AppConfigFile;
import dev.nocalhost.plugin.intellij.utils.KubeConfigUtil;
import lombok.SneakyThrows;

public class ConfigAppAction extends AnAction {
    private static final Logger LOG = Logger.getInstance(ConfigAppAction.class);

    private final Project project;
    private final ApplicationNode node;

    public ConfigAppAction(Project project, ApplicationNode node) {
        super("Config", "", AllIcons.Nodes.Editorconfig);
        this.project = project;
        this.node = node;
    }


    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Loading app config") {
            private String config;

            @Override
            public void onSuccess() {
                String filename = node.getApplication().getContext().getApplicationName() + ".yaml";
                VirtualFile virtualFile = new AppConfigFile(filename, config, project, node);
                OpenFileDescriptor openFileDescriptor = new OpenFileDescriptor(project, virtualFile, 0);
                FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
                fileEditorManager.openTextEditor(openFileDescriptor, true);
            }

            @Override
            public void onThrowable(@NotNull Throwable e) {
                LOG.error("error occurred while getting application config", e);
            }

            @SneakyThrows
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                final NhctlCommand nhctlCommand = ServiceManager.getService(NhctlCommand.class);

                NhctlConfigOptions nhctlConfigOptions = new NhctlConfigOptions();
                nhctlConfigOptions.setKubeconfig(KubeConfigUtil.kubeConfigPath(node.getDevSpace()).toString());
                nhctlConfigOptions.setAppConfig(true);
                config = nhctlCommand.getConfig(node.getApplication().getContext().getApplicationName(), nhctlConfigOptions);
            }
        });
    }
}
