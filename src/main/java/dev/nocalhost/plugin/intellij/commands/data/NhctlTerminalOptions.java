package dev.nocalhost.plugin.intellij.commands.data;

import dev.nocalhost.plugin.intellij.api.data.DevSpace;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NhctlTerminalOptions extends NhctlGlobalOptions {
    private String deployment;
    private String container;
    private String pod;

    public NhctlTerminalOptions(DevSpace devSpace) {
        super(devSpace);
    }
}
