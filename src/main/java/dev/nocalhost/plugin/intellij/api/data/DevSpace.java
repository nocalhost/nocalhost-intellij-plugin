package dev.nocalhost.plugin.intellij.api.data;

import com.google.gson.annotations.SerializedName;

import lombok.Data;

@Data
public class DevSpace {

    @SerializedName("cluster_id")
    private int clusterId;
    @SerializedName("context")
    private String contextStr;
    private transient Context context;
    private int cpu;
    @SerializedName("devspace_id")
    private int devSpaceId;
    private int id;
    @SerializedName("install_status")
    private int installStatus;
    @SerializedName("kubeconfig")
    private String kubeConfig;
    private int memory;
    private String namespace;
    @SerializedName("space_name")
    private String spaceName;
    private int status;
    @SerializedName("storage_class")
    private String storageClass;

    @Data
    public static class Context {
        private String source;
        @SerializedName("install_type")
        private String installType;
        @SerializedName("resource_dir")
        private String[] resourceDir;
        @SerializedName("application_name")
        private String applicationName;
        @SerializedName("application_url")
        private String applicationUrl;
        @SerializedName("application_config_path")
        private String applicationConfigPath;
        @SerializedName("nocalhost_config")
        private String nocalhostConfig;
    }
}