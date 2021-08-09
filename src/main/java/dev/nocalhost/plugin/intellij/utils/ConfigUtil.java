package dev.nocalhost.plugin.intellij.utils;

import com.google.common.collect.Lists;

import com.intellij.openapi.application.ApplicationManager;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import dev.nocalhost.plugin.intellij.commands.NhctlCommand;
import dev.nocalhost.plugin.intellij.commands.data.NhctlRenderOptions;
import dev.nocalhost.plugin.intellij.data.nocalhostconfig.NocalhostConfig;

public final class ConfigUtil {
    public static final Set<String> CONFIG_FILE_EXTENSIONS = Set.of("yaml", "yml");

    public static List<Path> resolveConfigFiles(Path configDirectory, Path kubeConfigPath, String namespace) throws IOException {
        if (Files.notExists(configDirectory)) {
            return Lists.newArrayList();
        }
        final NhctlCommand nhctlCommand = ApplicationManager.getApplication().getService(NhctlCommand.class);
        final NhctlRenderOptions opts = new NhctlRenderOptions(kubeConfigPath, namespace);
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
                        String content = nhctlCommand.render(e, opts);
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
