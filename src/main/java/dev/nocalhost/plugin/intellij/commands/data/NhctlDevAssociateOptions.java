package dev.nocalhost.plugin.intellij.commands.data;

import com.intellij.openapi.progress.Task;

import java.nio.file.Path;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NhctlDevAssociateOptions extends NhctlGlobalOptions {
    private String localSync;
    private String controllerType;
    private String deployment;
    private String container;
    private boolean info;
    private boolean migrate;

    public NhctlDevAssociateOptions(Path kubeConfigPath, String namespace) {
        super(kubeConfigPath, namespace);
    }

    public NhctlDevAssociateOptions(Path kubeConfigPath, String namespace, Task task) {
        super(kubeConfigPath, namespace, task);
    }
}
