package dev.nocalhost.plugin.intellij.commands.data;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NhctlUninstallOptions extends NhctlGlobalOptions {
    private boolean force;
}
