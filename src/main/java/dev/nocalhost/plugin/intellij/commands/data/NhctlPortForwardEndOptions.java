package dev.nocalhost.plugin.intellij.commands.data;

import java.nio.file.Path;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NhctlPortForwardEndOptions extends NhctlGlobalOptions {
    private String deployment;
    private String port;
    private String type;

    public NhctlPortForwardEndOptions(Path kubeConfigPath, String namespace) {
        super(kubeConfigPath, namespace);
    }
}
