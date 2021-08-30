package dev.nocalhost.plugin.intellij.commands.data;

import com.intellij.openapi.progress.Task;

import java.nio.file.Path;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NhctlUninstallOptions extends NhctlGlobalOptions {
    private boolean force;

    public NhctlUninstallOptions(Path kubeConfigPath, String namespace) {
        super(kubeConfigPath, namespace);
    }

    public NhctlUninstallOptions(Path kubeConfigPath, String namespace, Task task) {
        super(kubeConfigPath, namespace, task);
    }
}
