package dev.nocalhost.plugin.intellij.commands.data;

import dev.nocalhost.plugin.intellij.api.data.DevSpace;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NhctlPluginOptions extends NhctlGlobalOptions {
    private String deployment;

    public NhctlPluginOptions(DevSpace devSpace) {
        super(devSpace);
    }
}
