package dev.nocalhost.plugin.intellij.utils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import dev.nocalhost.plugin.intellij.api.data.DevSpace;

public class KubeConfigUtil {
    private static final Path KUBECONFIG_DIR = Paths.get(System.getProperty("user.home"), ".nh/intellij-plugin/kubeConfigs");

    public static Path kubeConfigPath(DevSpace devSpace) {
        Path path = KUBECONFIG_DIR.resolve(devSpace.getId() + "_" + devSpace.getDevSpaceId() + "_config");
        if (!Files.exists(path)) {
            try {
                Files.createDirectories(path.getParent());
                Files.write(path, devSpace.getKubeConfig().getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return path;
    }
}
