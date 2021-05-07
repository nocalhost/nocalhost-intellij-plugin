package dev.nocalhost.plugin.intellij.api.data;

import com.google.gson.annotations.SerializedName;

import java.util.List;

import lombok.Data;

@Data
public class ServiceAccount {
    @SerializedName("cluster_id")
    private int clusterId;

    @SerializedName("cluster_name")
    private String clusterName;

    @SerializedName("kubeconfig")
    private String kubeConfig;

    @SerializedName("storage_class")
    private String storageClass;

    @SerializedName("namespace_packs")
    private List<Namespace> namespaces;

    private boolean privilege;

    @Data
    public static class Namespace {
        @SerializedName("space_id")
        private int spaceId;

        private String namespace;

        @SerializedName("spacename")
        private String spaceName;
    }
}
