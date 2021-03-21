package dev.nocalhost.plugin.intellij.commands.data;

import dev.nocalhost.plugin.intellij.api.data.DevSpace;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NhctlPortForwardEndOptions extends NhctlGlobalOptions {
    private String deployment;
    private String port;
    private String type;

    public NhctlPortForwardEndOptions(DevSpace devSpace) {
        super(devSpace);
    }
}
