package dev.nocalhost.plugin.intellij.commands.data;

import dev.nocalhost.plugin.intellij.api.data.DevSpace;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NhctlConfigOptions extends NhctlGlobalOptions {
    private String deployment;
    private String content;
    private boolean appConfig;

    public NhctlConfigOptions(DevSpace devSpace) {
        super(devSpace);
    }
}
