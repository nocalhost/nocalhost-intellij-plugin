package dev.nocalhost.plugin.intellij.commands.data;

import java.nio.file.Path;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NhctlListPVCOptions extends NhctlGlobalOptions {
    private String app;
    private String controller;

    public NhctlListPVCOptions(Path kubeConfigPath, String namespace) {
        super(kubeConfigPath, namespace);
    }
}
