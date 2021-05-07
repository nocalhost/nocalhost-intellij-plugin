package dev.nocalhost.plugin.intellij.commands.data;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NhctlUpgradeOptions extends NhctlGlobalOptions {
    private String config;
    private String gitRef;
    private String gitUrl;
    private String helmChartName;
    private String helmRepoName;
    private String helmRepoUrl;
    private String helmRepoVersion;
    private String helmValues;
    private Map<String, String> values;
    private String localPath;
    private List<String> resourcesPath;

    public NhctlUpgradeOptions(Path kubeConfigPath, String namespace) {
        super(kubeConfigPath, namespace);
    }
}
