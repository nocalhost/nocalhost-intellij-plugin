package dev.nocalhost.plugin.intellij.utils;

import com.intellij.openapi.util.SystemInfo;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class PathsUtil {
    public static boolean isSame(String a, String b) {
        try {
            return Files.isSameFile(Paths.get(a), Paths.get(b));
        } catch (IOException ignore) {
            return false;
        }
    }

    public static boolean isDiff(String a, String b) {
        return !isSame(a, b);
    }

    public static boolean isExists(String path) {
        if (StringUtils.isEmpty(path)) {
            return false;
        }
        return Files.exists(Paths.get(path));
    }

    // https://nocalhost.coding.net/p/nocalhost/subtasks/issues/561/detail
    public static @NotNull String backslash(@NotNull String path) {
        if (SystemInfo.isWindows) {
            return path.replace("\\", "\\\\");
        }
        return path;
    }
}
