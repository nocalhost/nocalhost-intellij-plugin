package dev.nocalhost.plugin.intellij.commands.data;

import com.intellij.openapi.progress.Task;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NhctlInstallOptions extends NhctlGlobalOptions {
    private String config;
    private String gitRef;
    private String gitUrl;
    private String helmChartName;
    private String helmRepoName;
    private String helmRepoUrl;
    private String helmRepoVersion;
    private String helmValues;
    private boolean ignorePreInstall;
    private String outerConfig;
    private List<String> resourcesPath;
    private Map<String, String> values;
    private String type;
    private boolean wait;
    private String localPath;
    private boolean authCheck;

    public NhctlInstallOptions(Path kubeConfigPath, String namespace) {
        super(kubeConfigPath, namespace);
    }

    public NhctlInstallOptions(Path kubeConfigPath, String namespace, Task task) {
        super(kubeConfigPath, namespace, task);
    }
}
