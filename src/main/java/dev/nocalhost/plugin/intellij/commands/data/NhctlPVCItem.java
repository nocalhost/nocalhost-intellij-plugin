package dev.nocalhost.plugin.intellij.commands.data;

import com.google.gson.annotations.SerializedName;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NhctlPVCItem {
    private String name;
    @SerializedName("app_name")
    private String appName;
    @SerializedName("service_name")
    private String serviceName;
    private String capacity;
    @SerializedName("storage_class")
    private String storageClass;
    private String status;
    @SerializedName("mount_path")
    private String mountPath;
}
