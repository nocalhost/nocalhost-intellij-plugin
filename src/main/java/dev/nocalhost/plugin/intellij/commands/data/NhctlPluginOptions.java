package dev.nocalhost.plugin.intellij.commands.data;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NhctlPluginOptions extends NhctlGlobalOptions {
    private String deployment;
}
