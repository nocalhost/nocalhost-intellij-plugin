package dev.nocalhost.plugin.intellij.data;

import java.nio.file.Path;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class ServiceProjectPath {
    private Path kubeConfigPath;
    private String namespace;
    private String applicationName;
    private String serviceName;
    private String serviceType;
    private String containerName;
    private String sha;
}
