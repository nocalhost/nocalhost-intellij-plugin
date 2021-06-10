package dev.nocalhost.plugin.intellij.commands.data;

import java.nio.file.Path;

public class NhctlPortForwardListOptions extends NhctlGlobalOptions {
    public NhctlPortForwardListOptions(Path kubeConfigPath, String namespace) {
        super(kubeConfigPath, namespace);
    }
}
