package dev.nocalhost.plugin.intellij.commands.data;

import java.nio.file.Path;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NhctlExecOptions extends NhctlGlobalOptions {
    private List<String> command;
    private String deployment;

    public NhctlExecOptions(Path kubeConfigPath, String namespace) {
        super(kubeConfigPath, namespace);
    }
}
