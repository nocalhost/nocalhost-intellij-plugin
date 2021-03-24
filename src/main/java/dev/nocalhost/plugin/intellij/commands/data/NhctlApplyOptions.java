package dev.nocalhost.plugin.intellij.commands.data;

import dev.nocalhost.plugin.intellij.api.data.DevSpace;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NhctlApplyOptions extends NhctlGlobalOptions {
    private String file;

    public NhctlApplyOptions(DevSpace devSpace) {
        super(devSpace);
    }
}
