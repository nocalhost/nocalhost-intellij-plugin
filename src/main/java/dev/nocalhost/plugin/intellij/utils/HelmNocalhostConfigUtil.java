package dev.nocalhost.plugin.intellij.utils;

import com.google.common.collect.Maps;

import org.apache.commons.codec.binary.StringUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

import dev.nocalhost.plugin.intellij.api.data.Application;

public class HelmNocalhostConfigUtil {
    private static final Path HELM_CONFIGS_DIR = Paths.get(
            System.getProperty("user.home"),
            ".nh/intellij-plugin/helmNHConfigs");

    private static final Map<String, Path> nocalhostConfigPathMap = Maps.newHashMap();

    public static Path helmNocalhostConfigPath(Application application) {
        String nocalhostConfig = application.getContext().getNocalhostConfig();
        synchronized (nocalhostConfigPathMap) {
            try {
                if (!nocalhostConfigPathMap.containsKey(nocalhostConfig)) {
                    Path path;
                    while (true) {
                        path = HELM_CONFIGS_DIR.resolve(UUID.randomUUID().toString() + "_config");
                        if (!Files.exists(path)) {
                            nocalhostConfigPathMap.put(nocalhostConfig, path);
                            break;
                        }
                    }
                }
                Path path = nocalhostConfigPathMap.get(nocalhostConfig);
                if (!Files.exists(path)) {
                    Files.createDirectories(path.getParent());
                    Files.write(path, nocalhostConfig.getBytes(StandardCharsets.UTF_8));
                    path.toFile().deleteOnExit();
                } else {
                    String content = new String(Files.readAllBytes(path));
                    if (!StringUtils.equals(content, nocalhostConfig)) {
                        Files.write(path, nocalhostConfig.getBytes(StandardCharsets.UTF_8));
                    }
                }
                return path.toAbsolutePath();
            } catch (Exception e) {
                throw new RuntimeException("Preparing nocalhost config file error", e);
            }
        }
    }
}
