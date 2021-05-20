package dev.nocalhost.plugin.intellij.utils;

import com.intellij.openapi.util.SystemInfo;

import java.nio.file.Path;
import java.nio.file.Paths;

public final class NhctlUtil {
    private static final Path NOCALHOST_BIN_PATH = Paths.get(System.getProperty("user.home"), ".nh", "bin");

    public static String parentDir() {
        return NOCALHOST_BIN_PATH.toString();
    }

    public static String getName() {
        if (SystemInfo.isWindows) {
            return "nhctl.exe";
        } else {
            return "nhctl";
        }
    }

    public static String binaryPath() {
        return NOCALHOST_BIN_PATH.resolve(getName()).toAbsolutePath().toString();
    }
}
