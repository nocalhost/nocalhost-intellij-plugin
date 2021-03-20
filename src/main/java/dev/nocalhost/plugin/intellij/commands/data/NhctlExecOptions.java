package dev.nocalhost.plugin.intellij.commands.data;

import java.util.List;

import dev.nocalhost.plugin.intellij.api.data.DevSpace;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NhctlExecOptions extends NhctlGlobalOptions {
    private List<String> command;
    private String deployment;

    public NhctlExecOptions(DevSpace devSpace) {
        super(devSpace);
    }
}
