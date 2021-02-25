package dev.nocalhost.plugin.intellij.commands.data;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NhctlResetOptions extends NhctlGlobalOptions {
    private String deployment;
}
