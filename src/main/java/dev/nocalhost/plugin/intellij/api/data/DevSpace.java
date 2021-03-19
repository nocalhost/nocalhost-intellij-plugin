package dev.nocalhost.plugin.intellij.api.data;

import com.google.gson.annotations.SerializedName;

import lombok.Data;

@Data
public class DevSpace {
    private int id;
    @SerializedName("cluster_id")
    private int clusterId;
    @SerializedName("user_id")
    private int userId;
    @SerializedName("application_id")
    private int applicationId;
    @SerializedName("space_name")
    private String spaceName;
    @SerializedName("kubeconfig")
    private String kubeConfig;
    private int memory;
    private int cpu;
    @SerializedName("space_resource_limit")
    private String spaceResourceLimitStr;
    private transient SpaceResourceLimit spaceResourceLimit;
    private String namespace;
    private int status;
    @SerializedName("created_at")
    private String createdAt;
    @SerializedName("storage_class")
    private String storageClass;
    @SerializedName("dev_start_append_command")
    private String[] devStartAppendCommand;

    @Data
    public static class SpaceResourceLimit {

        @SerializedName("container_ephemeral_storage")
        private String containerEphemeralStorage;
        @SerializedName("container_limits_cpu")
        private String containerLimitsCpu;
        @SerializedName("container_limits_mem")
        private String containerLimitsMem;
        @SerializedName("container_req_cpu")
        private String containerReqCpu;
        @SerializedName("container_req_mem")
        private String containerReqMem;
        @SerializedName("space_ephemeral_storage")
        private String spaceEphemeralStorage;
        @SerializedName("space_lb_count")
        private String spaceLbCount;
        @SerializedName("space_limits_cpu")
        private String spaceLimitsCpu;
        @SerializedName("space_limits_mem")
        private String spaceLimitsMem;
        @SerializedName("space_pvc_count")
        private String spacePvcCount;
        @SerializedName("space_req_cpu")
        private String spaceReqCpu;
        @SerializedName("space_req_mem")
        private String spaceReqMem;
        @SerializedName("space_storage_capacity")
        private String spaceStorageCapacity;
    }
}
