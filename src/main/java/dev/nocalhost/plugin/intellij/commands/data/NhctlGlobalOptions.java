package dev.nocalhost.plugin.intellij.commands.data;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class NhctlGlobalOptions {
    private boolean debug;
    private String kubeconfig;
}
