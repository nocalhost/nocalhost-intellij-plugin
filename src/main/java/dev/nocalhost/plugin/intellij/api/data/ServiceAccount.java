package dev.nocalhost.plugin.intellij.api.data;

import com.google.gson.annotations.SerializedName;

import java.util.List;

import lombok.Data;

@Data
public class ServiceAccount {
    @SerializedName("cluster_id")
    private long clusterId;

    @SerializedName("cluster_name")
    private String clusterName;

    @SerializedName("kubeconfig")
    private String kubeConfig;

    @SerializedName("storage_class")
    private String storageClass;

    @SerializedName("namespace_packs")
    private List<NamespacePack> namespacePacks;

    private boolean privilege;

    @SerializedName("privilege_type")
    private String privilegeType;

    @Data
    public static class NamespacePack {
        @SerializedName("space_id")
        private long spaceId;

        private String namespace;

        @SerializedName("spacename")
        private String spaceName;

        @SerializedName("space_own_type")
        private String spaceOwnType;
    }
}
