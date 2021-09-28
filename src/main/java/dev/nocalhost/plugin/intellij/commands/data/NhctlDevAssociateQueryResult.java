package dev.nocalhost.plugin.intellij.commands.data;

import com.google.gson.annotations.SerializedName;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NhctlDevAssociateQueryResult {
    @SerializedName("svc_pack")
    private ServicePack servicePack;
    @SerializedName("kubeconfig_path")
    private String kubeconfigPath;
    @SerializedName("syncthing_status")
    private SyncthingStatus syncthingStatus;

    private String sha;
    private String server;

    @Getter
    @Setter
    public class ServicePack {
        @SerializedName("ns")
        private String namespace;
        @SerializedName("app")
        private String applicationName;
        @SerializedName("svc_type")
        private String serviceType;
        @SerializedName("svc")
        private String serviceName;
        private String container;
    }

    @Getter
    @Setter
    public class SyncthingStatus {
        private String status;
        @SerializedName("msg")
        private String message;
    }
}
