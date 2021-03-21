package dev.nocalhost.plugin.intellij.commands.data;

import dev.nocalhost.plugin.intellij.api.data.DevSpace;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NhctlListPVCOptions extends NhctlGlobalOptions {
    private String app;
    private String svc;

    public NhctlListPVCOptions(DevSpace devSpace) {
        super(devSpace);
    }
}
