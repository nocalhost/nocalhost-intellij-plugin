package dev.nocalhost.plugin.intellij.ui.vfs;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.impl.FileDocumentManagerImpl;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileSystem;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Date;

import dev.nocalhost.plugin.intellij.commands.NhctlCommand;
import dev.nocalhost.plugin.intellij.commands.data.NhctlDescribeOptions;
import dev.nocalhost.plugin.intellij.commands.data.NhctlDescribeService;
import dev.nocalhost.plugin.intellij.exception.NocalhostNotifier;
import dev.nocalhost.plugin.intellij.nhctl.NhctlConfigEditCommand;
import dev.nocalhost.plugin.intellij.task.BaseBackgroundTask;
import dev.nocalhost.plugin.intellij.ui.tree.node.ResourceNode;
import dev.nocalhost.plugin.intellij.utils.ErrorUtil;
import dev.nocalhost.plugin.intellij.utils.KubeConfigUtil;
import lombok.SneakyThrows;

public class ConfigFile extends VirtualFile {
    private final NhctlCommand nhctlCommand = ApplicationManager.getApplication().getService(NhctlCommand.class);

    private final String name;
    private final String path;
    private String content;
    private final Project project;
    private final ResourceNode node;

    private final Path kubeConfigPath;
    private final String namespace;

    public ConfigFile(String name, String path, String content, Project project, ResourceNode node) {
        this.name = name;
        this.path = path;
        this.node = node;
        this.content = content;
        this.project = project;

        kubeConfigPath = KubeConfigUtil.toPath(node.getClusterNode().getRawKubeConfig());
        namespace = node.getNamespaceNode().getNamespace();
    }

    @Override
    public @NotNull @NlsSafe String getName() {
        return name;
    }

    @Override
    public @NotNull VirtualFileSystem getFileSystem() {
        return new ConfigFileSystem();
    }

    @Override
    public @NonNls
    @NotNull String getPath() {
        return path;
    }

    @Override
    public boolean isWritable() {
        return !isReadonly(node.getNhctlDescribeService());
    }

    @Override
    public boolean isDirectory() {
        return false;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public VirtualFile getParent() {
        return null;
    }

    @Override
    public VirtualFile[] getChildren() {
        return new VirtualFile[0];
    }

    @Override
    public @NotNull OutputStream getOutputStream(Object requestor, long newModificationStamp, long newTimeStamp) throws IOException {
        String newContent = ((FileDocumentManagerImpl) requestor).getDocument(this).getText();
        content = newContent;
        saveContent(newContent);
        OutputStream outputStream = new ByteArrayOutputStream();
        outputStream.write(newContent.getBytes());
        return outputStream;
    }

    protected void saveContent(String newContent) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                NhctlDescribeOptions nhctlDescribeOptions = new NhctlDescribeOptions(kubeConfigPath, namespace);
                nhctlDescribeOptions.setDeployment(node.resourceName());
                nhctlDescribeOptions.setType(node.controllerType());
                NhctlDescribeService nhctlDescribeService = nhctlCommand.describe(node.applicationName(), nhctlDescribeOptions, NhctlDescribeService.class);

                if (isReadonly(nhctlDescribeService)) {
                    Messages.showMessageDialog("Config cannot be modified.", "Modify Config", null);
                    return;
                }

                ApplicationManager.getApplication().invokeLater(() -> {
                    ProgressManager.getInstance().run(new BaseBackgroundTask(null, "Saving " + name) {
                        @Override
                        public void onSuccess() {
                            super.onSuccess();
                            NocalhostNotifier.getInstance(project).notifySuccess(name + " saved", "");
                        }

                        @Override
                        public void onThrowable(@NotNull Throwable ex) {
                            ErrorUtil.dealWith(project, "Failed to save dev config",
                                    "Error occurred while saving dev config", ex);
                        }

                        @SneakyThrows
                        @Override
                        public void runTask(@NotNull ProgressIndicator indicator) {
                            var cmd = new NhctlConfigEditCommand(project);
                            cmd.setYaml(newContent);
                            cmd.setKubeConfig(kubeConfigPath);
                            cmd.setDeployment(node.resourceName());
                            cmd.setApplication(node.applicationName());
                            cmd.setControllerType(node.controllerType());
                            cmd.setNamespace(node.getNamespaceNode().getNamespace());
                            cmd.execute();
                        }
                    });
                });
            } catch (Exception e) {
                ErrorUtil.dealWith(project, "Loading service status error",
                        "Error occurs while loading service status", e);
            }
        });
    }

    @Override
    public byte @NotNull [] contentsToByteArray() throws IOException {
        return content.getBytes();
    }

    @Override
    public long getTimeStamp() {
        return new Date().getTime();
    }

    @Override
    public long getLength() {
        return content.getBytes().length;
    }

    @Override
    public void refresh(boolean asynchronous, boolean recursive, @Nullable Runnable postRunnable) {

    }

    @Override
    public InputStream getInputStream() throws IOException {
        return new ByteArrayInputStream(content.getBytes());
    }

    @Override
    public long getModificationStamp() {
        return new Date().getTime();
    }

    private boolean isReadonly(NhctlDescribeService nhctlDescribeService) {
        return nhctlDescribeService.isCmconfigloaded() || nhctlDescribeService.isLocalconfigloaded() || nhctlDescribeService.isAnnotationsconfigloaded();
    }
}
