package dev.nocalhost.plugin.intellij.commands.data;

import java.nio.file.Path;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NhctlPortForwardStartOptions extends NhctlGlobalOptions {
    private boolean daemon;
    private String deployment;
    private List<String> devPorts;
    private String pod;
    private String type;

    public NhctlPortForwardStartOptions(Path kubeConfigPath, String namespace) {
        super(kubeConfigPath, namespace);
    }
}
