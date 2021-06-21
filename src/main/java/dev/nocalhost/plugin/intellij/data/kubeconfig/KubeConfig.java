package dev.nocalhost.plugin.intellij.data.kubeconfig;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class KubeConfig {
    private List<KubeCluster> clusters;
    private List<KubeContext> contexts;
    private List<KubeUser> users;

    private String apiVersion = "v1";
    private String kind = "Config";
    private Map<String, Object> preferences;

    @SerializedName("current-context")
    private String currentContext = "";
}
