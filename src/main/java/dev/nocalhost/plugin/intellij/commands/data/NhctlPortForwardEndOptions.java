package dev.nocalhost.plugin.intellij.commands.data;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NhctlPortForwardEndOptions extends NhctlGlobalOptions {
    private String deployment;
    private String port;
}
