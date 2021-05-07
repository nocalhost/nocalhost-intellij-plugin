package dev.nocalhost.plugin.intellij.commands.data;

import java.nio.file.Path;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class NhctlSyncResumeOptions extends NhctlGlobalOptions {

    private String deployment;

    public NhctlSyncResumeOptions(Path kubeConfigPath, String namespace) {
        super(kubeConfigPath, namespace);
    }
}
