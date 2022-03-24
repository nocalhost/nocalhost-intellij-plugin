package dev.nocalhost.plugin.intellij.commands.data;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ServiceContainerDevDebug {
    private String remoteDebugPort;
    private String language;
}
