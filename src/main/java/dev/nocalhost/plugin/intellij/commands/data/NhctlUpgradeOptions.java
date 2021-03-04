package dev.nocalhost.plugin.intellij.commands.data;

import java.util.List;

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
    private String localPath;
    private List<String> resourcesPath;
}
