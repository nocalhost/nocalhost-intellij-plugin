package dev.nocalhost.plugin.intellij.commands.data;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NhctlSyncStatusOptions extends NhctlPluginOptions {
    private String deployment;
}
