package dev.nocalhost.plugin.intellij.settings.data;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DevModeService {
    private String image;
    private String action;
    private String server;
    private String username;
    private long clusterId;
    private String rawKubeConfig;
    private String namespace;
    private String applicationName;
    private String serviceName;
    private String serviceType;
    private String containerName;
    private String projectPath;
}
