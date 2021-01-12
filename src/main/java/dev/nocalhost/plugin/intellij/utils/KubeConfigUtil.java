package dev.nocalhost.plugin.intellij.utils;

import java.nio.file.Path;
import java.nio.file.Paths;

import dev.nocalhost.plugin.intellij.api.data.DevSpace;

public class KubeConfigUtil {
    private static final Path KUBECONFIG_DIR = Paths.get(System.getProperty("user.home"), ".nh/intellij-plugin/kubeConfigs");

    public static Path kubeConfigPath(DevSpace devSpace) {
        return KUBECONFIG_DIR.resolve(devSpace.getId() + "_" + devSpace.getDevSpaceId() + "_config");
    }
}
