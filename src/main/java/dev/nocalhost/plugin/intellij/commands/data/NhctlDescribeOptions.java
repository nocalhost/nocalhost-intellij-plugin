package dev.nocalhost.plugin.intellij.commands.data;

import com.intellij.openapi.progress.Task;

import java.nio.file.Path;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NhctlDescribeOptions extends NhctlGlobalOptions {
    private String deployment;
    private String type;

    public NhctlDescribeOptions(Path kubeConfigPath, String namespace) {
        super(kubeConfigPath, namespace);
    }

    public NhctlDescribeOptions(Path kubeConfigPath, String namespace, Task task) {
        super(kubeConfigPath, namespace, task);
    }
}
