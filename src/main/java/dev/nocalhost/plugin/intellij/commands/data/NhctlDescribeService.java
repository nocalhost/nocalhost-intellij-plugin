package dev.nocalhost.plugin.intellij.commands.data;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NhctlDescribeService {
    private NhctlRawConfig rawConfig;
    private String actualName;
    private boolean portForwarded;
    private boolean syncing;
    private List<String> localAbsoluteSyncDirFromDevStartPlugin;
    private List<NhctlPortForward> devPortForwardList;
    private String associate;
    private boolean developing;
    private boolean possess;
    private boolean localconfigloaded;
}
