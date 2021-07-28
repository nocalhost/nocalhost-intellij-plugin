package dev.nocalhost.plugin.intellij.commands.data;

import java.nio.file.Path;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NhctlSshReverseOptions extends NhctlGlobalOptions {
    private String pod;
    private String port;
    private String local;
    private String remote;

    public NhctlSshReverseOptions(Path kubeConfigPath, String namespace) {
        super(kubeConfigPath, namespace);
    }
}

