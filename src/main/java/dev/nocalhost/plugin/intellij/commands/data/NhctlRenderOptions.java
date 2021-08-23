package dev.nocalhost.plugin.intellij.commands.data;

import java.nio.file.Path;

public class NhctlRenderOptions extends NhctlGlobalOptions {
    public NhctlRenderOptions(Path kubeConfigPath, String namespace) {
        super(kubeConfigPath, namespace);
    }
}
