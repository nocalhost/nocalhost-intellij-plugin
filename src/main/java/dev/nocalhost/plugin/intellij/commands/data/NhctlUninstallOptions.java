package dev.nocalhost.plugin.intellij.commands.data;

import dev.nocalhost.plugin.intellij.api.data.DevSpace;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NhctlUninstallOptions extends NhctlGlobalOptions {
    private boolean force;

    public NhctlUninstallOptions(DevSpace devSpace) {
        super(devSpace);
    }
}
