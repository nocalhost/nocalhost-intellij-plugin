package dev.nocalhost.plugin.intellij.ui.vfs;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileListener;
import com.intellij.openapi.vfs.VirtualFileSystem;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

public class KubeConfigFileSystem extends VirtualFileSystem {
    @Override
    public @NonNls
    @NotNull String getProtocol() {
        return this.getClass().getCanonicalName();
    }

    @Override
    public @Nullable VirtualFile findFileByPath(@NotNull @NonNls String path) {
        return null;
    }

    @Override
    public void refresh(boolean asynchronous) {

    }

    @Override
    public @Nullable VirtualFile refreshAndFindFileByPath(@NotNull String path) {
        return null;
    }

    @Override
    public void addVirtualFileListener(@NotNull VirtualFileListener listener) {

    }

    @Override
    public void removeVirtualFileListener(@NotNull VirtualFileListener listener) {

    }

    @Override
    protected void deleteFile(Object requestor, @NotNull VirtualFile vFile) throws IOException {
        throw new IOException("unsupported deleting file");
    }

    @Override
    protected void moveFile(Object requestor, @NotNull VirtualFile vFile, @NotNull VirtualFile newParent) throws IOException {
        throw new IOException("unsupported moving file");
    }

    @Override
    protected void renameFile(Object requestor, @NotNull VirtualFile vFile, @NotNull String newName) throws IOException {
        throw new IOException("unsupported renaming file");
    }

    @Override
    protected @NotNull VirtualFile createChildFile(Object requestor, @NotNull VirtualFile vDir, @NotNull String fileName) throws IOException {
        throw new IOException("unsupported creating child file");
    }

    @Override
    protected @NotNull VirtualFile createChildDirectory(Object requestor, @NotNull VirtualFile vDir, @NotNull String dirName) throws IOException {
        throw new IOException("unsupported creating child directory");
    }

    @Override
    protected @NotNull VirtualFile copyFile(Object requestor, @NotNull VirtualFile virtualFile, @NotNull VirtualFile newParent, @NotNull String copyName) throws IOException {
        throw new IOException("unsupported coping file");
    }

    @Override
    public boolean isReadOnly() {
        return true;
    }
}
