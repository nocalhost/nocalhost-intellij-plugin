package dev.nocalhost.plugin.intellij.ui.vfs;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.impl.FileDocumentManagerImpl;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
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
import dev.nocalhost.plugin.intellij.commands.data.NhctlConfigOptions;
import dev.nocalhost.plugin.intellij.exception.NocalhostNotifier;
import dev.nocalhost.plugin.intellij.task.BaseBackgroundTask;
import dev.nocalhost.plugin.intellij.ui.tree.node.ApplicationNode;
import dev.nocalhost.plugin.intellij.utils.DataUtils;
import dev.nocalhost.plugin.intellij.utils.ErrorUtil;
import dev.nocalhost.plugin.intellij.utils.KubeConfigUtil;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;

@AllArgsConstructor
public class AppConfigFile extends VirtualFile {
    private final String name;
    private String content;
    private final Project project;
    private final ApplicationNode node;

    @Override
    public @NotNull @NlsSafe String getName() {
        return name;
    }

    @Override
    public @NotNull VirtualFileSystem getFileSystem() {
        return AppConfigFileSystem.INSTANCE;
    }

    @Override
    public @NonNls
    @NotNull String getPath() {
        return name;
    }

    @Override
    public boolean isWritable() {
        return true;
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
        Object yml = DataUtils.YAML.load(newContent);
        String json = DataUtils.GSON.toJson(yml);
        ProgressManager.getInstance().run(new BaseBackgroundTask(null, "Saving " + name) {
            @Override
            public void onSuccess() {
                super.onSuccess();
                NocalhostNotifier.getInstance(project).notifySuccess(name + " saved", "");
            }

            @Override
            public void onThrowable(@NotNull Throwable e) {
                ErrorUtil.dealWith(project, "Nocalhost save config error",
                        "Error occurred while saving config file", e);
            }

            @SneakyThrows
            @Override
            public void runTask(@NotNull ProgressIndicator indicator) {
                final NhctlCommand nhctlCommand = ApplicationManager.getApplication().getService(NhctlCommand.class);
                Path path = KubeConfigUtil.kubeConfigPath(node.getClusterNode().getRawKubeConfig());
                String namespace = node.getNamespaceNode().getNamespace();
                NhctlConfigOptions nhctlConfigOptions = new NhctlConfigOptions(path, namespace, this);
                nhctlConfigOptions.setContent(Base64.getEncoder().encodeToString(json.getBytes()));
                nhctlConfigOptions.setAppConfig(true);
                nhctlCommand.editConfig(node.getName(), nhctlConfigOptions);
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
}
