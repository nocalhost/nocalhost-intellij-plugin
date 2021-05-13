package dev.nocalhost.plugin.intellij.commands.data;

import java.nio.file.Path;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NhctlDevAssociateOptions extends NhctlGlobalOptions {
    private String associate;
    private String controllerType;
    private String deployment;

    public NhctlDevAssociateOptions(Path kubeConfigPath, String namespace) {
        super(kubeConfigPath, namespace);
    }
}
