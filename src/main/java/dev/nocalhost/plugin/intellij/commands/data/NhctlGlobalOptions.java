package dev.nocalhost.plugin.intellij.commands.data;

import java.nio.file.Path;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class NhctlGlobalOptions {
    private boolean debug;
    private String kubeconfig;
    private String namespace;

    protected NhctlGlobalOptions(Path kubeConfigPath, String namespace) {
        this.kubeconfig = kubeConfigPath.toString();
        this.namespace = namespace;
    }
}
