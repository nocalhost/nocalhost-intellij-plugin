package dev.nocalhost.plugin.intellij.commands.data;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NhctlPluginInfo {
    private String name;
    private String releasename;
    private String namespace;
    private String kubeconfig;
    private String dependencyConfigMapName;
    private String appType;

    private NhctlSvcProfile[] svcProfile;
}
