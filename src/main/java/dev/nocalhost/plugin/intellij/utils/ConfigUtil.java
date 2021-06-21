package dev.nocalhost.plugin.intellij.utils;

import com.google.common.collect.Lists;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import dev.nocalhost.plugin.intellij.data.NocalhostConfig;

public final class ConfigUtil {
    public static final Set<String> CONFIG_FILE_EXTENSIONS = Set.of("yaml", "yml");

    public static List<Path> resolveConfigFiles(Path configDirectory) throws IOException {
        if (Files.notExists(configDirectory)) {
            return Lists.newArrayList();
        }
        return Files.list(configDirectory)
                .filter(Files::isRegularFile)
                .filter(e ->
                        CONFIG_FILE_EXTENSIONS.contains(
                                com.google.common.io.Files.getFileExtension(
                                        e.getFileName().toString())
                        )
                )
                .filter(e -> {
                    try {
                        String content = Files.readString(e);
                        NocalhostConfig nc = DataUtils.YAML.loadAs(content, NocalhostConfig.class);
                        if (nc.getApplication() != null
                                && StringUtils.isNotEmpty(nc.getApplication().getName())) {
                            return true;
                        }
                    } catch (Exception ignored) {
                    }
                    return false;
                })
                .map(Path::toAbsolutePath)
                .collect(Collectors.toList());
    }
}
