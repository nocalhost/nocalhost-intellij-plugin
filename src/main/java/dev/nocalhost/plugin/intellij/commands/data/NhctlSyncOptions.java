package dev.nocalhost.plugin.intellij.commands.data;

import java.nio.file.Path;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NhctlSyncOptions extends NhctlGlobalOptions {
    private String deployment;
    private String controllerType;
    private String container;
    private boolean doubleSide;
    private boolean overwrite;
    private boolean resume;
    private boolean stop;
    private List<String> ignoredPatterns;
    private List<String> syncedPatterns;

    public NhctlSyncOptions(Path kubeConfigPath, String namespace) {
        super(kubeConfigPath, namespace);
    }
}
