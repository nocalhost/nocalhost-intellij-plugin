package dev.nocalhost.plugin.intellij.ui.action.workload;

import com.intellij.icons.AllIcons;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageDialogBuilder;
import com.intellij.openapi.vfs.VirtualFile;

import org.apache.http.client.utils.URIBuilder;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

import dev.nocalhost.plugin.intellij.commands.NhctlCommand;
import dev.nocalhost.plugin.intellij.commands.data.NhctlConfigOptions;
import dev.nocalhost.plugin.intellij.task.BaseBackgroundTask;
import dev.nocalhost.plugin.intellij.i18n.NocalhostI18n;
import dev.nocalhost.plugin.intellij.ui.tree.node.ResourceNode;
import dev.nocalhost.plugin.intellij.ui.vfs.ConfigFile;
import dev.nocalhost.plugin.intellij.utils.ErrorUtil;
import dev.nocalhost.plugin.intellij.utils.KubeConfigUtil;
import lombok.SneakyThrows;

public class ConfigAction extends DumbAwareAction {
    private final NhctlCommand nhctlCommand = ApplicationManager.getApplication().getService(NhctlCommand.class);

    private final Project project;
    private final ResourceNode node;
    private final Path kubeConfigPath;
    private final String namespace;

    public ConfigAction(Project project, ResourceNode node) {
        super(NocalhostI18n.get("action.devConfig"), "", AllIcons.Nodes.Editorconfig);
        this.project = project;
        this.node = node;
        this.kubeConfigPath = KubeConfigUtil.toPath(node.getClusterNode().getRawKubeConfig());
        this.namespace = node.getNamespaceNode().getNamespace();
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        ProgressManager.getInstance().run(new BaseBackgroundTask(project, NocalhostI18n.get("progress.loadingConfig")) {
            private String config;

            @Override
            public void onSuccess() {
                super.onSuccess();
                String filename = node.resourceName() + ".yaml";
                VirtualFile virtualFile = new ConfigFile(filename, filename, config, project, node);
                OpenFileDescriptor openFileDescriptor = new OpenFileDescriptor(project, virtualFile, 0);
                FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
                fileEditorManager.openTextEditor(openFileDescriptor, true);

                var yes = MessageDialogBuilder.yesNo(
                        NocalhostI18n.get("action.devConfig"),
                        NocalhostI18n.get("confirm.openDevConfigTools")
                ).ask(project);
                if (yes) {
                    openDevConfigTools();
                }
            }

            @Override
            public void onThrowable(@NotNull Throwable e) {
                ErrorUtil.dealWith(project, NocalhostI18n.get("error.getAppConfig"),
                        NocalhostI18n.get("error.getAppConfig.content"), e);
            }

            @SneakyThrows
            @Override
            public void runTask(@NotNull ProgressIndicator indicator) {
                NhctlConfigOptions nhctlConfigOptions = new NhctlConfigOptions(kubeConfigPath, namespace, this);
                nhctlConfigOptions.setDeployment(node.resourceName());
                nhctlConfigOptions.setControllerType(node.controllerType());
                config = nhctlCommand.getConfig(node.applicationName(), nhctlConfigOptions);
            }
        });
    }

    private void openDevConfigTools() {
        try {
            var x = new URIBuilder("https://nocalhost.dev/tools");
            x.addParameter("from", "daemon");
            x.addParameter("name", node.resourceName());
            x.addParameter("type", node.controllerType());
            x.addParameter("namespace", namespace);
            x.addParameter("kubeconfig", kubeConfigPath.toString());
            x.addParameter("application", node.applicationName());
            BrowserUtil.browse(x.build().toString());
        } catch (Exception ex) {
            ErrorUtil.dealWith(project, NocalhostI18n.get("error.openBrowser"),
                    NocalhostI18n.get("error.openBrowser.content"), ex);
        }
    }
}
