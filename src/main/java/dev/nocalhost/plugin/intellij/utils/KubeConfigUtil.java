package dev.nocalhost.plugin.intellij.utils;

import org.jetbrains.annotations.NotNull;
import com.intellij.openapi.util.SystemInfo;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.codec.digest.DigestUtils;

import java.util.Set;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;

public class KubeConfigUtil {
    private static final Path KUBE_CONFIGS_DIR = Paths.get(
            System.getProperty("user.home"),
            ".nh/intellij-plugin/kubeConfigs");

    private static final FileAttribute<Set<PosixFilePermission>> FILE_MODE = PosixFilePermissions
            .asFileAttribute(PosixFilePermissions.fromString("rw-------"));

    public synchronized static Path toPath(String kubeConfig) {
        try {
            var hex = DigestUtils.md5Hex(compress(kubeConfig));
            var path = KUBE_CONFIGS_DIR.resolve(hex + "_config");
            if (Files.exists(path)) {
                var text = new String(Files.readAllBytes(path));
                if ( ! StringUtils.equals(text, kubeConfig)) {
                    Files.write(path, kubeConfig.getBytes(StandardCharsets.UTF_8));
                }
            } else {
                Files.createDirectories(path.getParent());
                if ( ! SystemInfo.isWindows) {
                    Files.createFile(path, FILE_MODE);
                }
                Files.write(path, kubeConfig.getBytes(StandardCharsets.UTF_8));
                path.toFile().deleteOnExit();
            }
            return path.toAbsolutePath();
        } catch (Exception ex) {
            throw new RuntimeException("Failed to resolve path for KubeConfig", ex);
        }
    }

    public static @NotNull String compress(@NotNull String raw) {
        return raw.replaceAll("[\\s\\t\\n\\r]", "");
    }
}
