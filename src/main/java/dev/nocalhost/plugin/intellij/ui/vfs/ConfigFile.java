package dev.nocalhost.plugin.intellij.ui.vfs;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.impl.FileDocumentManagerImpl;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
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
import java.util.Base64;
import java.util.Date;

import dev.nocalhost.plugin.intellij.commands.NhctlCommand;
import dev.nocalhost.plugin.intellij.commands.OutputCapturedNhctlCommand;
import dev.nocalhost.plugin.intellij.commands.data.NhctlConfigOptions;
import dev.nocalhost.plugin.intellij.commands.data.NhctlDescribeOptions;
import dev.nocalhost.plugin.intellij.commands.data.NhctlDescribeService;
import dev.nocalhost.plugin.intellij.exception.NocalhostNotifier;
import dev.nocalhost.plugin.intellij.ui.tree.node.ResourceNode;
import dev.nocalhost.plugin.intellij.utils.DataUtils;
import dev.nocalhost.plugin.intellij.utils.ErrorUtil;
import dev.nocalhost.plugin.intellij.utils.KubeConfigUtil;
import lombok.SneakyThrows;

public class ConfigFile extends VirtualFile {
    private static final Logger LOG = Logger.getInstance(ConfigFile.class);

    private final NhctlCommand nhctlCommand = ServiceManager.getService(NhctlCommand.class);
    private final OutputCapturedNhctlCommand outputCapturedNhctlCommand;

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
        this.content = content;
        this.project = project;
        this.node = node;

        outputCapturedNhctlCommand = project.getService(OutputCapturedNhctlCommand.class);

        kubeConfigPath = KubeConfigUtil.kubeConfigPath(node.getClusterNode().getRawKubeConfig());
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
                nhctlDescribeOptions.setType(node.getKubeResource().getKind());
                NhctlDescribeService nhctlDescribeService = nhctlCommand.describe(node.applicationName(), nhctlDescribeOptions, NhctlDescribeService.class);

                if (isReadonly(nhctlDescribeService)) {
                    Messages.showMessageDialog("Config cannot be modified.", "Modify Config", null);
                    return;
                }

                Object yml = DataUtils.YAML.load(newContent);
                String json = DataUtils.GSON.toJson(yml);
                ApplicationManager.getApplication().invokeLater(() -> {
                    ProgressManager.getInstance().run(new Task.Backgroundable(null, "Saving " + name, false) {
                        @Override
                        public void onSuccess() {
                            NocalhostNotifier.getInstance(project).notifySuccess(name + " saved", "");
                        }

                        @Override
                        public void onThrowable(@NotNull Throwable e) {
                            LOG.error("error occurred while saving config file", e);
                            NocalhostNotifier.getInstance(project).notifyError("Nocalhost save config error", "Error occurred while saving config file", e.getMessage());
                        }

                        @SneakyThrows
                        @Override
                        public void run(@NotNull ProgressIndicator indicator) {
                            NhctlConfigOptions nhctlConfigOptions = new NhctlConfigOptions(kubeConfigPath, node.getNamespaceNode().getNamespace());
                            nhctlConfigOptions.setDeployment(node.resourceName());
                            nhctlConfigOptions.setControllerType(node.getKubeResource().getKind());
                            nhctlConfigOptions.setContent(Base64.getEncoder().encodeToString(json.getBytes()));
                            outputCapturedNhctlCommand.editConfig(node.applicationName(), nhctlConfigOptions);
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
