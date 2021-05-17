package dev.nocalhost.plugin.intellij.commands.data;

import java.nio.file.Path;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NhctlResetServiceOptions extends NhctlGlobalOptions {
    private String deployment;
    private String controllerType;

    public NhctlResetServiceOptions(Path kubeConfigPath, String namespace) {
        super(kubeConfigPath, namespace);
    }
}
