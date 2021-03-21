package dev.nocalhost.plugin.intellij.commands.data;

import dev.nocalhost.plugin.intellij.api.data.DevSpace;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NhctlDescribeOptions extends NhctlGlobalOptions {
    private String deployment;
    private String type;

    public NhctlDescribeOptions(DevSpace devSpace) {
        super(devSpace);
    }
}
