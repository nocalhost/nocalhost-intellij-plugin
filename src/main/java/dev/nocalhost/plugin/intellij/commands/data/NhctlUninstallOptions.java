package dev.nocalhost.plugin.intellij.commands.data;

import java.nio.file.Path;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NhctlUninstallOptions extends NhctlGlobalOptions {
    private boolean force;

    public NhctlUninstallOptions(Path kubeConfigPath, String namespace) {
        super(kubeConfigPath, namespace);
    }
}
