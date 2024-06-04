package dev.nocalhost.plugin.intellij.utils;

import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class FileChooseUtil {
    public static FileChooserDescriptor singleFileChooserDescriptor() {
        FileChooserDescriptor fileChooserDescriptor = new FileChooserDescriptor(true, false, false, false, false, false);
        return fileChooserDescriptor;
    }

    public static FileChooserDescriptor singleDirectoryChooserDescriptor() {
        FileChooserDescriptor fileChooserDescriptor = new FileChooserDescriptor(false, true, false, false, false, false);
        return fileChooserDescriptor;
    }

    public static Path chooseSingleFile(Project project) {
        FileChooserDescriptor fileChooserDescriptor = new FileChooserDescriptor(true, false, false, false, false, false)
                .withFileFilter(f -> !f.isDirectory());
        VirtualFile virtualFile = FileChooser.chooseFile(fileChooserDescriptor, project, null);
        if (virtualFile == null) {
            return null;
        }
        return virtualFile.toNioPath().toAbsolutePath();
    }

    public static Path chooseSingleFile(Project project, String title, Path root, Set<String> filenames) {
        FileChooserDescriptor fileChooserDescriptor = new FileChooserDescriptor(true, false, false, false, false, false)
                .withTitle(title)
                .withRoots(LocalFileSystem.getInstance().findFileByNioFile(root))
                .withFileFilter(f -> !f.isDirectory() && filenames.contains(f.getName()));
        VirtualFile virtualFile = FileChooser.chooseFile(fileChooserDescriptor, project, null);
        if (virtualFile == null) {
            return null;
        }
        return virtualFile.toNioPath().toAbsolutePath();
    }

    public static Path chooseSingleDirectory(Project project) {
        FileChooserDescriptor fileChooserDescriptor = new FileChooserDescriptor(false, true, false, false, false, false)
                .withFileFilter(VirtualFile::isDirectory);
        VirtualFile virtualFile = FileChooser.chooseFile(fileChooserDescriptor, project, null);
        if (virtualFile == null) {
            return null;
        }
        return virtualFile.toNioPath().toAbsolutePath();
    }

    public static Path chooseSingleDirectory(Project project, String title, String message) {
        FileChooserDescriptor fileChooserDescriptor = new FileChooserDescriptor(false, true, false, false, false, false)
                .withTitle(title)
                .withDescription(message)
                .withFileFilter(VirtualFile::isDirectory);
        VirtualFile virtualFile = FileChooser.chooseFile(fileChooserDescriptor, project, null);
        if (virtualFile == null) {
            return null;
        }
        return virtualFile.toNioPath().toAbsolutePath();
    }

    public static Path chooseSingleFileOrDirectory(Project project) {
        FileChooserDescriptor fileChooserDescriptor = new FileChooserDescriptor(true, true, false, false, false, false);
        VirtualFile virtualFile = FileChooser.chooseFile(fileChooserDescriptor, project, null);
        if (virtualFile == null) {
            return null;
        }
        return virtualFile.toNioPath().toAbsolutePath();
    }

    public static List<Path> chooseFilesAndDirectories(Project project) {
        FileChooserDescriptor fileChooserDescriptor = new FileChooserDescriptor(true, true, false, false, false, true);
        VirtualFile[] virtualFiles = FileChooser.chooseFiles(fileChooserDescriptor, project, null);
        return Arrays.stream(virtualFiles).map(e -> e.toNioPath()).collect(Collectors.toList());
    }
}
