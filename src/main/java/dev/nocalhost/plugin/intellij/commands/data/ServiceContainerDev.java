package dev.nocalhost.plugin.intellij.commands.data;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ServiceContainerDev {
    private String gitUrl;
    private String image;
    private String shell;
    private String workDir;
    private ServiceContainerDevCommand command;
    private ServiceContainerDevDebug debug;
    private boolean hotReload;
    private boolean useDevContainer;
    private List<String> portForward;
}
