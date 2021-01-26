package dev.nocalhost.plugin.intellij.commands.data;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NhctlPVCItem {
    private String name;
    private String appName;
    private String serviceName;
    private String capacity;
    private String storageClass;
    private String status;
    private String mountPath;
}
