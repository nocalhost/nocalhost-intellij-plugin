package dev.nocalhost.plugin.intellij.data.kubeconfig;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class KubeContext {
    private String name;
    private Context context;

    @Getter
    @Setter
    public static class Context {
        private String cluster;
        private String namespace;
        private String user;
    }
}
