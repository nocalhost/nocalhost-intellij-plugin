package dev.nocalhost.plugin.intellij.commands.data;

import java.nio.file.Path;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NhctlSyncStatusOptions extends NhctlGlobalOptions {
    private String deployment;
    private String controllerType;
    private boolean override;
    private boolean wait;

    public NhctlSyncStatusOptions(Path kubeConfigPath, String namespace) {
        super(kubeConfigPath, namespace);
    }
}
