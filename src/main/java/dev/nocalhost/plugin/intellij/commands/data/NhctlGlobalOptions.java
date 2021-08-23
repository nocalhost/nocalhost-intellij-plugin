package dev.nocalhost.plugin.intellij.commands.data;

import com.intellij.openapi.progress.Task;

import java.nio.file.Path;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class NhctlGlobalOptions {
    private boolean debug;
    private String kubeconfig;
    private String namespace;
    private Task task;

    protected NhctlGlobalOptions(Path kubeConfigPath) {
        this.kubeconfig = kubeConfigPath.toString();
    }

    protected NhctlGlobalOptions(Path kubeConfigPath, String namespace) {
        this(kubeConfigPath);
        this.namespace = namespace;
    }

    protected NhctlGlobalOptions(Path kubeConfigPath, String namespace, Task task) {
        this(kubeConfigPath, namespace);
        this.task = task;
    }
}
