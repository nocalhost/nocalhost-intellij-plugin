package dev.nocalhost.plugin.intellij.commands.data;

import com.intellij.openapi.progress.Task;

import java.nio.file.Path;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NhctlConfigOptions extends NhctlGlobalOptions {
    private String deployment;
    private String content;
    private boolean appConfig;
    private String controllerType;

    public NhctlConfigOptions(Path kubeConfigPath, String namespace) {
        super(kubeConfigPath, namespace);
    }

    public NhctlConfigOptions(Path kubeConfigPath, String namespace, Task task) {
        super(kubeConfigPath, namespace, task);
    }
}
