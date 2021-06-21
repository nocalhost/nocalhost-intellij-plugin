package dev.nocalhost.plugin.intellij.data.kubeconfig;

import java.util.Map;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class KubeCluster {
    private String name;
    private Map<String, Object> cluster;
}
