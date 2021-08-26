package dev.nocalhost.plugin.intellij.commands.data;

import com.google.gson.annotations.SerializedName;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NhctlDevAssociateQueryResult {
    @SerializedName("svc_pack")
    private ServicePack servicePack;
    private String sha;
    @SerializedName("kubeconfig_path")
    private String kubeconfigPath;

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
}
