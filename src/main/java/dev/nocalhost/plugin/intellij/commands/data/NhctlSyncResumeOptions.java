package dev.nocalhost.plugin.intellij.commands.data;

import dev.nocalhost.plugin.intellij.api.data.DevSpace;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class NhctlSyncResumeOptions extends NhctlGlobalOptions {

    private String deployment;

    public NhctlSyncResumeOptions(DevSpace devSpace) {
        super(devSpace);
    }
}
