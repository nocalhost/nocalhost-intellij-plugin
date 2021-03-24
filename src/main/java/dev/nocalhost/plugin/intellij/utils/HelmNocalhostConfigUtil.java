package dev.nocalhost.plugin.intellij.utils;

import org.apache.commons.codec.binary.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import dev.nocalhost.plugin.intellij.api.data.Application;
import dev.nocalhost.plugin.intellij.api.data.DevSpace;

public class HelmNocalhostConfigUtil {
    private static final Path HELM_CONFIGS_DIR = Paths.get(
            System.getProperty("user.home"),
            ".nh/intellij-plugin/helmNHConfigs");

    public static Path helmNocalhostConfigPath(DevSpace devSpace, Application application) {
        Path path = HELM_CONFIGS_DIR.resolve(devSpace.getApplicationId() + "_" + devSpace.getId() + "_config");
        try {
            if (!Files.exists(path)) {
                Files.createDirectories(path.getParent());
                Files.write(path, application.getContext().getNocalhostConfig().getBytes(StandardCharsets.UTF_8));
            } else {
                String currentKubeConfig = new String(Files.readAllBytes(path));
                if (!StringUtils.equals(currentKubeConfig, devSpace.getKubeConfig())) {
                    Files.write(path, application.getContext().getNocalhostConfig().getBytes(StandardCharsets.UTF_8));
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return path;
    }
}
