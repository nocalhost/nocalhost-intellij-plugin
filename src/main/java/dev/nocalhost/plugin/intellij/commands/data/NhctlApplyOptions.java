package dev.nocalhost.plugin.intellij.commands.data;

import java.nio.file.Path;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NhctlApplyOptions extends NhctlGlobalOptions {
    private String file;

    public NhctlApplyOptions(Path kubeConfigPath, String namespace) {
        super(kubeConfigPath, namespace);
    }
}
