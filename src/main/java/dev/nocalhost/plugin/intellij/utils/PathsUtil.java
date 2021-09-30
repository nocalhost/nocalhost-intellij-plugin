package dev.nocalhost.plugin.intellij.utils;

import com.intellij.openapi.util.SystemInfo;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class PathsUtil {
    public static boolean isSame(String a, String b) {
        try {
            return Files.isSameFile(Paths.get(a), Paths.get(b));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // https://nocalhost.coding.net/p/nocalhost/subtasks/issues/561/detail
    public static @NotNull String backslash(@NotNull String path) {
        if (SystemInfo.isWindows) {
            return path.replace("\\", "\\\\");
        }
        return path;
    }
}
