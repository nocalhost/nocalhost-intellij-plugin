package dev.nocalhost.plugin.intellij.commands.data;

import java.nio.file.Path;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NhctlConfigOptions extends NhctlGlobalOptions {
    private String deployment;
    private String content;
    private boolean appConfig;

    public NhctlConfigOptions(Path kubeConfigPath, String namespace) {
        super(kubeConfigPath, namespace);
    }
}
