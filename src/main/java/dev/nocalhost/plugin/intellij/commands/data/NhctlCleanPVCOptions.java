package dev.nocalhost.plugin.intellij.commands.data;

import dev.nocalhost.plugin.intellij.api.data.DevSpace;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NhctlCleanPVCOptions extends NhctlGlobalOptions {
    private String app;
    private String svc;
    private String name;

    public NhctlCleanPVCOptions(DevSpace devSpace) {
        super(devSpace);
    }
}
