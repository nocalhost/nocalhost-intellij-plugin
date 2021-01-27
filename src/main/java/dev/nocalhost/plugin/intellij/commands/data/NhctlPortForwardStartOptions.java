package dev.nocalhost.plugin.intellij.commands.data;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NhctlPortForwardStartOptions extends NhctlGlobalOptions {
    private boolean daemon;
    private String deployment;
    private List<String> devPorts;
}
