package dev.nocalhost.plugin.intellij.commands.data;

import java.nio.file.Path;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NhctlProfileGetOptions extends NhctlGlobalOptions {
    private String container;
    private String deployment;
    private String key;
    private String type;

    public NhctlProfileGetOptions(Path kubeConfigPath, String namespace) {
        super(kubeConfigPath, namespace);
    }
}
