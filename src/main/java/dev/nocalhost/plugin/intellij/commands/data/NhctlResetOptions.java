package dev.nocalhost.plugin.intellij.commands.data;

import java.nio.file.Path;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NhctlResetOptions extends NhctlGlobalOptions {
    private String deployment;

    public NhctlResetOptions(Path kubeConfigPath, String namespace) {
        super(kubeConfigPath, namespace);
    }
}
