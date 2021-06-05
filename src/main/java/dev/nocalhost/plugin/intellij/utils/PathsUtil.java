package dev.nocalhost.plugin.intellij.utils;

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
}
