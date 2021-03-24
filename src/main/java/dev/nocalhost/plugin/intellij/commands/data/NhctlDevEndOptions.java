package dev.nocalhost.plugin.intellij.commands.data;

import dev.nocalhost.plugin.intellij.api.data.DevSpace;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NhctlDevEndOptions extends NhctlGlobalOptions {
    private String deployment;

    public NhctlDevEndOptions(DevSpace devSpace) {
        super(devSpace);
    }
}
