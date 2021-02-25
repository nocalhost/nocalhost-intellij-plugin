package dev.nocalhost.plugin.intellij.commands.data;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Deprecated
public class NhctlSvcProfile {
    private String name;
    private String serviceType;
    private String gitURL;
    private String devContainerImage;
    private String workDir;
    private String[] syncDirs;
    private String[] devPorts;
    private boolean developing;
    private boolean portForwarded;
    private boolean syncing;
    private String[] localAbsoluteSyncDirFromDevStartPlugin;
    private String[] devPortList;
    private String[] syncFilePattern;
    private String[] ignoreFilePattern;
}
