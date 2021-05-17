package dev.nocalhost.plugin.intellij.commands.data;

import java.nio.file.Path;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NhctlResetDevSpaceOptions extends NhctlGlobalOptions {
    public NhctlResetDevSpaceOptions(Path kubeConfigPath, String namespace) {
        super(kubeConfigPath, namespace);
    }
}
