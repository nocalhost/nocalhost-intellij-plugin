package dev.nocalhost.plugin.intellij.commands.data;

import java.nio.file.Path;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NhctlProfileSetOptions extends NhctlGlobalOptions {
    private String deployment;
    private String type;
    private String container;
    private String key;
    private String value;

    public NhctlProfileSetOptions(Path kubeConfigPath, String namespace) {
        super(kubeConfigPath, namespace);
    }
}
