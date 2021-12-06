package dev.nocalhost.plugin.intellij.commands.data;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NhctlDescribeService {
    private String actualName;
    private boolean portForwarded;
    private boolean syncing;
    private List<String> localAbsoluteSyncDirFromDevStartPlugin;
    private List<NhctlPortForward> devPortForwardList;
    private String associate;
    private boolean possess;
    private boolean localconfigloaded;
    private boolean cmconfigloaded;
    private boolean annotationsconfigloaded;
    private String develop_status;
    private String devModeType;
}
