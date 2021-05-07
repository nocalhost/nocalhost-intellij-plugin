package dev.nocalhost.plugin.intellij.commands.data;

import java.nio.file.Path;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NhctlTerminalOptions extends NhctlGlobalOptions {
    private String deployment;
    private String container;
    private String pod;

    public NhctlTerminalOptions(Path kubeConfigPath, String namespace) {
        super(kubeConfigPath, namespace);
    }
}
