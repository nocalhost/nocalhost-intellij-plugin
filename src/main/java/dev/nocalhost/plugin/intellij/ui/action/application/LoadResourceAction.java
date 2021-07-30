package dev.nocalhost.plugin.intellij.ui.action.application;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

import dev.nocalhost.plugin.intellij.commands.NhctlCommand;
import dev.nocalhost.plugin.intellij.commands.data.NhctlDescribeOptions;
import dev.nocalhost.plugin.intellij.exception.NocalhostExecuteCmdException;
import dev.nocalhost.plugin.intellij.ui.tree.node.ApplicationNode;
import dev.nocalhost.plugin.intellij.ui.vfs.ReadOnlyVirtualFile;
import dev.nocalhost.plugin.intellij.utils.KubeConfigUtil;
import lombok.SneakyThrows;

public class LoadResourceAction extends DumbAwareAction {
    private static final Logger LOG = Logger.getInstance(LoadResourceAction.class);

    private final Project project;
    private final Path kubeConfigPath;
    private final String namespace;
    private final String applicationName;

    public LoadResourceAction(Project project, ApplicationNode node) {
        super("Load Resource");
        this.project = project;
        this.kubeConfigPath = KubeConfigUtil.kubeConfigPath(node.getClusterNode().getRawKubeConfig());
        this.namespace = node.getNamespaceNode().getNamespace();
        this.applicationName = node.getName();
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Loading kubernetes resources") {
            private VirtualFile virtualFile;

            @Override
            public void onSuccess() {
                FileEditorManager.getInstance(project).openTextEditor(new OpenFileDescriptor(project, virtualFile, 0), true);
            }

            @Override
            public void onThrowable(@NotNull Throwable e) {
                if (e instanceof NocalhostExecuteCmdException) {
                    return;
                }
                LOG.error("error occurred while describing application", e);
            }

            @SneakyThrows
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                final NhctlCommand nhctlCommand = ServiceManager.getService(NhctlCommand.class);

                NhctlDescribeOptions opts = new NhctlDescribeOptions(kubeConfigPath, namespace);
                String resource = nhctlCommand.describe(applicationName, opts);
                String filename = applicationName + ".yaml";
                virtualFile = new ReadOnlyVirtualFile(filename, filename, resource);
            }
        });

    }
}
