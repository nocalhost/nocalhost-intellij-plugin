package dev.nocalhost.plugin.intellij.commands.data;

import java.nio.file.Path;

public class NhctlCheckClusterOptions extends NhctlGlobalOptions {
    public NhctlCheckClusterOptions(Path kubeConfigPath) {
        super(kubeConfigPath);
    }
}
