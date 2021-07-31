package dev.nocalhost.plugin.intellij.ui.vfs;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.impl.FileDocumentManagerImpl;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageDialogBuilder;
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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;

import dev.nocalhost.plugin.intellij.commands.NhctlCommand;
import dev.nocalhost.plugin.intellij.commands.data.NhctlApplyOptions;
import dev.nocalhost.plugin.intellij.exception.NocalhostNotifier;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;

@AllArgsConstructor
public class KubeConfigFile extends VirtualFile {
    private static final Logger LOG = Logger.getInstance(KubeConfigFile.class);

    private final String name;
    private final String path;
    private final String resourceName;
    private String content;
    private final Project project;
    private final String appName;
    private final Path kubeConfigPath;
    private final String namespace;

    @Override
    public @NotNull @NlsSafe String getName() {
        return name;
    }

    @Override
    public @NotNull VirtualFileSystem getFileSystem() {
        return new KubeConfigFileSystem();
    }

    @Override
    public @NonNls
    @NotNull String getPath() {
        return path;
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

    private void saveContent(String newContent) {
        ApplicationManager.getApplication().invokeLater(() -> {
            if (!MessageDialogBuilder.yesNo("Edit Manifest", "Apply this resource?").ask(project)) {
                return;
            }
            ProgressManager.getInstance().run(new Task.Backgroundable(null, "Applying " + name, false) {

                String result = "";

                @Override
                public void onSuccess() {
                    NocalhostNotifier.getInstance(project).notifySuccess(name + " applied", result);
                }

                @Override
                public void onThrowable(@NotNull Throwable e) {
                    LOG.error("error occurred while apply config file", e);
                    NocalhostNotifier.getInstance(project).notifyError("Nocalhost apply error", "Error occurred while applying file", e.getMessage());
                }

                @SneakyThrows
                @Override
                public void run(@NotNull ProgressIndicator indicator) {
                    Path tempFile = Files.createTempFile(resourceName, ".yaml");
                    Files.write(tempFile, newContent.getBytes(StandardCharsets.UTF_8));
                    final NhctlCommand nhctlCommand = ApplicationManager.getApplication().getService(NhctlCommand.class);
                    NhctlApplyOptions nhctlApplyOptions = new NhctlApplyOptions(kubeConfigPath, namespace);
                    nhctlApplyOptions.setFile(tempFile.toAbsolutePath().toString());
                    result = nhctlCommand.apply(appName, nhctlApplyOptions);
                }
            });
        });
    }

    @Override
    public byte @NotNull [] contentsToByteArray() throws IOException {
        return content.getBytes();
    }

    @Override
    public long getModificationStamp() {
        return new Date().getTime();
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
}
