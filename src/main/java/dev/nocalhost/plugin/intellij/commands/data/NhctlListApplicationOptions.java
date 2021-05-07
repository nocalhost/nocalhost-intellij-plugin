package dev.nocalhost.plugin.intellij.commands.data;

import java.nio.file.Path;

public class NhctlListApplicationOptions extends NhctlGlobalOptions {

    public NhctlListApplicationOptions(Path kubeConfigPath, String namespace) {
        super(kubeConfigPath, namespace);
    }
}
