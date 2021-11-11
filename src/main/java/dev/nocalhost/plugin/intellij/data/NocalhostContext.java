package dev.nocalhost.plugin.intellij.data;

import java.nio.file.Path;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class NocalhostContext {
    private final Path kubeConfigPath;
    private final String applicationName;
    private final String containerName;
    private final String serviceName;
    private final String serviceType;
    private final String namespace;
    private final String server;
    private final String sha;
}
