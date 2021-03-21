package dev.nocalhost.plugin.intellij.commands.data;

import dev.nocalhost.plugin.intellij.api.data.DevSpace;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NhctlSyncStatusOptions extends NhctlGlobalOptions {
    private String deployment;

    public NhctlSyncStatusOptions(DevSpace devSpace) {
        super(devSpace);
    }
}
