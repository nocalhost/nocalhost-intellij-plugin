package dev.nocalhost.plugin.intellij.commands.data;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ServiceContainerDev {
    private String gitUrl;
    private String shell;
    private List<String> portForward;
}
