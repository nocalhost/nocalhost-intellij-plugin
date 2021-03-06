package dev.nocalhost.plugin.intellij.commands.data;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ServiceContainer {
    private String name;
    private ServiceContainerDev dev;
    private ServiceContainerInstall install;

}
