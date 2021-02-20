package dev.nocalhost.plugin.intellij.commands.data;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NhctlDescribeAllService {
    private String name;
    private String releasename;
    private String namespace;
    private String kubeconfig;
    private String dependencyConfigMapName;
    private String appType;
    private NhctlDescribeService[] svcProfile;
    private boolean installed;
    private String[] syncDirs;
    private String[] resourcePath;
    private String[] ignoredPath;
}
