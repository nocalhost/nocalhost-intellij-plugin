package dev.nocalhost.plugin.intellij.task;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

import dev.nocalhost.plugin.intellij.commands.KubectlCommand;
import dev.nocalhost.plugin.intellij.exception.NocalhostNotifier;
import dev.nocalhost.plugin.intellij.ui.tree.node.ResourceNode;
import dev.nocalhost.plugin.intellij.ui.vfs.KubeConfigFile;
import dev.nocalhost.plugin.intellij.utils.KubeConfigUtil;
import lombok.SneakyThrows;

public class LoadKubernetesResourceTask extends Task.Backgroundable {
    private static final Logger LOG = Logger.getInstance(LoadKubernetesResourceTask.class);

    private final KubectlCommand kubectlCommand = ServiceManager.getService(KubectlCommand.class);

    private final ResourceNode node;
    private final Path kubeConfigPath;
    private final String namespace;

    private VirtualFile virtualFile;

    public LoadKubernetesResourceTask(Project project, ResourceNode node) {
        super(project, "Loading kubernetes resource");
        this.node = node;
        this.kubeConfigPath = KubeConfigUtil.kubeConfigPath(node.getClusterNode().getRawKubeConfig());
        this.namespace = node.getNamespaceNode().getName();
    }

    @Override
    public void onSuccess() {
        FileEditorManager.getInstance(getProject()).openTextEditor(new OpenFileDescriptor(getProject(), virtualFile, 0), true);
    }

    @Override
    public void onThrowable(@NotNull Throwable e) {
        LOG.error("error occurred while loading kubernetes resource yaml", e);
        NocalhostNotifier.getInstance(getProject()).notifyError("Nocalhost load kubernetes resource error", "Error occurred while loading kubernetes resource yaml", e.getMessage());
    }

    @SneakyThrows
    @Override
    public void run(@NotNull ProgressIndicator indicator) {
        String content = kubectlCommand.getResourceYaml(
                node.getKubeResource().getKind(),
                node.getKubeResource().getMetadata().getName(),
                kubeConfigPath,
                namespace);
        virtualFile = new KubeConfigFile(
                node.resourceName() + ".yaml",
                node.resourceName() + ".yaml",
                node.resourceName(),
                content,
                getProject(),
                node.applicationName(),
                kubeConfigPath,
                node.getNamespaceNode().getName());
    }
}
