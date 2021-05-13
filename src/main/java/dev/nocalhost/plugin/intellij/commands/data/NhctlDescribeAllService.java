package dev.nocalhost.plugin.intellij.commands.data;

import java.util.List;

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
    private List<NhctlDescribeService> svcProfile;
    private boolean installed;
    private List<String> syncDirs;
    private List<String> resourcePath;
    private List<String> ignoredPath;
}
