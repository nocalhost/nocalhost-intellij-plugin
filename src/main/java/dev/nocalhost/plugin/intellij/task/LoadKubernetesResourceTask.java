package dev.nocalhost.plugin.intellij.task;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

import dev.nocalhost.plugin.intellij.commands.NhctlCommand;
import dev.nocalhost.plugin.intellij.commands.data.NhctlGetOptions;
import dev.nocalhost.plugin.intellij.ui.tree.node.ResourceNode;
import dev.nocalhost.plugin.intellij.ui.vfs.KubeConfigFile;
import dev.nocalhost.plugin.intellij.utils.DataUtils;
import dev.nocalhost.plugin.intellij.utils.ErrorUtil;
import dev.nocalhost.plugin.intellij.utils.KubeConfigUtil;
import lombok.SneakyThrows;

public class LoadKubernetesResourceTask extends BaseBackgroundTask {
    private final NhctlCommand nhctlCommand = ApplicationManager.getApplication().getService(NhctlCommand.class);

    private final ResourceNode node;
    private final Path kubeConfigPath;
    private final String namespace;

    private VirtualFile virtualFile;

    public LoadKubernetesResourceTask(Project project, ResourceNode node) {
        super(project, "Loading kubernetes resource");
        this.node = node;
        this.kubeConfigPath = KubeConfigUtil.kubeConfigPath(node.getClusterNode().getRawKubeConfig());
        this.namespace = node.getNamespaceNode().getNamespace();
    }

    @Override
    public void onSuccess() {
        super.onSuccess();
        FileEditorManager.getInstance(getProject()).openTextEditor(new OpenFileDescriptor(getProject(), virtualFile, 0), true);
    }

    @Override
    public void onThrowable(@NotNull Throwable e) {
        ErrorUtil.dealWith(this.getProject(), "Nocalhost load kubernetes resource error",
                "Error occurred while loading kubernetes resource yaml", e);
    }

    @SneakyThrows
    @Override
    public void runTask(@NotNull ProgressIndicator indicator) {
        NhctlGetOptions opts = new NhctlGetOptions(kubeConfigPath, namespace, this);
        String output = nhctlCommand.get(node.controllerType(), opts);
        JsonElement resources = DataUtils.GSON.fromJson(output, JsonElement.class);
        JsonObject selectedResource = null;
        for (JsonElement resource : resources.getAsJsonArray()) {
            String name = resource.getAsJsonObject()
                    .get("info").getAsJsonObject()
                    .get("metadata").getAsJsonObject()
                    .get("name").getAsString();
            if (StringUtils.equals(name, node.resourceName())) {
                selectedResource = resource.getAsJsonObject().get("info").getAsJsonObject();
                break;
            }
        }
        if (selectedResource == null) {
            throw new RuntimeException("Resource not found");
        }

        String content = DataUtils.toYaml(selectedResource);
        virtualFile = new KubeConfigFile(
                node.resourceName() + ".yaml",
                node.resourceName() + ".yaml",
                node.resourceName(),
                content,
                getProject(),
                node.applicationName(),
                kubeConfigPath,
                node.getNamespaceNode().getNamespace());
    }
}
