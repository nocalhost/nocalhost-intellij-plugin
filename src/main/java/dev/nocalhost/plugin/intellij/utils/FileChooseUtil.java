package dev.nocalhost.plugin.intellij.utils;

import com.google.common.io.Files;

import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;

import java.nio.file.Path;
import java.util.Set;

public final class FileChooseUtil {
    public static Path chooseSingleFile(Project project, String title) {
        FileChooserDescriptor fileChooserDescriptor = new FileChooserDescriptor(true, false, false, false, false, false)
                .withTitle(title)
                .withFileFilter(f -> !f.isDirectory());
        fileChooserDescriptor.setForcedToUseIdeaFileChooser(true);
        VirtualFile virtualFile = FileChooser.chooseFile(fileChooserDescriptor, project, null);
        if (virtualFile == null) {
            return null;
        }
        return virtualFile.toNioPath().toAbsolutePath();
    }

    public static Path chooseSingleFile(Project project, String title, Path root, Set<String> extensions) {
        FileChooserDescriptor fileChooserDescriptor = new FileChooserDescriptor(true, false, false, false, false, false)
                .withTitle(title)
                .withRoots(LocalFileSystem.getInstance().findFileByNioFile(root))
                .withFileFilter(f -> !f.isDirectory() && extensions.contains(Files.getFileExtension(f.getName())));
        fileChooserDescriptor.setForcedToUseIdeaFileChooser(true);
        VirtualFile virtualFile = FileChooser.chooseFile(fileChooserDescriptor, project, null);
        if (virtualFile == null) {
            return null;
        }
        return virtualFile.toNioPath().toAbsolutePath();
    }

    public static Path chooseSingleDirectory(Project project, String title) {
        FileChooserDescriptor fileChooserDescriptor = new FileChooserDescriptor(false, true, false, false, false, false)
                .withTitle(title)
                .withFileFilter(VirtualFile::isDirectory);
        fileChooserDescriptor.setForcedToUseIdeaFileChooser(true);
        VirtualFile virtualFile = FileChooser.chooseFile(fileChooserDescriptor, project, null);
        if (virtualFile == null) {
            return null;
        }
        return virtualFile.toNioPath().toAbsolutePath();
    }
}
