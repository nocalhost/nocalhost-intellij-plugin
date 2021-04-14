package dev.nocalhost.plugin.intellij.api.data;

import com.google.gson.annotations.SerializedName;

import lombok.Data;

@Data
public class ServiceAccount {
    @SerializedName("kubeconfig")
    private String kubeConfig;
    @SerializedName("storage_class")
    private String storageClass;
    private boolean privilege;
    private Namespace[] ns;

    @Data
    public static class Namespace {
        private String namespace;
        @SerializedName("spacename")
        private String spaceName;
    }
}
