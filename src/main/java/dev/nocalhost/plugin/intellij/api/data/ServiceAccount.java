package dev.nocalhost.plugin.intellij.api.data;

import com.google.gson.annotations.SerializedName;

import org.apache.commons.lang3.StringUtils;

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

    @SerializedName("kubeconfig_type")
    private String kubeConfigType;

    @SerializedName("virtual_cluster")
    private VirtualCluster virtualCluster;

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

        @SerializedName("sleep_status")
        private String SleepStatus;
    }

    @Data
    public static class VirtualCluster {
        @SerializedName("service_type")
        private String type;

        @SerializedName("service_port")
        private String port;

        @SerializedName("host_cluster_context")
        private String context;

        @SerializedName("service_address")
        private String address;

        @SerializedName("service_namespace")
        private String namespace;
    }

    public boolean isVirtualCluster() {
        return StringUtils.equals(kubeConfigType, "vcluster");
    }

    public boolean isClusterIP() {
        return virtualCluster != null && StringUtils.equals(virtualCluster.getType(), "ClusterIP");
    }
}
