package dev.nocalhost.plugin.intellij.utils;

import com.intellij.util.EnvironmentUtil;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class ExecutableUtil {
    public static String lookup(String command) {
        String paths = EnvironmentUtil.getEnvironmentMap().get("PATH");
        for (String path : paths.split(File.pathSeparator)) {
            Path commandPath = Paths.get(path, command);
            if (Files.exists(commandPath)) {
                return commandPath.toAbsolutePath().toString();
            }
        }
        return "";
    }
}
