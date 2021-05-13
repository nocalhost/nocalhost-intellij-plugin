package dev.nocalhost.plugin.intellij.settings.data;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceProjectPath {
    private String server;
    private String username;
    private String rawKubeConfig;
    private String namespace;
    private String applicationName;
    private String serviceName;
    private String containerName;
    private String imageUrl;

    private String projectPath;

    public void setProjectPath(String projectPath) {
        this.projectPath = projectPath;
    }
}
