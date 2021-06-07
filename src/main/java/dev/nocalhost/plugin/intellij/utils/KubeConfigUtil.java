package dev.nocalhost.plugin.intellij.utils;

import com.google.common.collect.Maps;

import com.intellij.openapi.util.SystemInfo;

import org.apache.commons.codec.binary.StringUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class KubeConfigUtil {
    private static final Path KUBE_CONFIGS_DIR = Paths.get(
            System.getProperty("user.home"),
            ".nh/intellij-plugin/kubeConfigs");

    private static final FileAttribute<Set<PosixFilePermission>> FILE_MODE = PosixFilePermissions
            .asFileAttribute(PosixFilePermissions.fromString("rw-------"));

    private static final Map<String, Path> kubeConfigPathMap = Maps.newHashMap();

    public static Path kubeConfigPath(String kubeConfig) {
        synchronized (kubeConfigPathMap) {
            try {
                if (!kubeConfigPathMap.containsKey(kubeConfig)) {
                    Path path;
                    while (true) {
                        path = KUBE_CONFIGS_DIR.resolve(UUID.randomUUID().toString() + "_config");
                        if (!Files.exists(path)) {
                            kubeConfigPathMap.put(kubeConfig, path);
                            break;
                        }
                    }
                }
                Path path = kubeConfigPathMap.get(kubeConfig);
                if (!Files.exists(path)) {
                    Files.createDirectories(path.getParent());
                    if (!SystemInfo.isWindows) {
                        Files.createFile(path, FILE_MODE);
                    }
                    Files.write(path, kubeConfig.getBytes(StandardCharsets.UTF_8));
                    path.toFile().deleteOnExit();
                } else {
                    String content = new String(Files.readAllBytes(path));
                    if (!StringUtils.equals(content, kubeConfig)) {
                        Files.write(path, kubeConfig.getBytes(StandardCharsets.UTF_8));
                    }
                }
                return path.toAbsolutePath();
            } catch (Exception e) {
                throw new RuntimeException("Preparing kubeconfig file error", e);
            }
        }
    }
}
