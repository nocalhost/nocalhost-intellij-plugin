package dev.nocalhost.plugin.intellij.ui.tree.node;

import org.apache.commons.lang3.StringUtils;

import javax.swing.tree.DefaultMutableTreeNode;

import dev.nocalhost.plugin.intellij.api.data.ServiceAccount;
import dev.nocalhost.plugin.intellij.data.kubeconfig.KubeConfig;
import dev.nocalhost.plugin.intellij.settings.data.NocalhostAccount;
import lombok.Getter;
import lombok.Setter;

@Getter
public class ClusterNode extends DefaultMutableTreeNode {
    private NocalhostAccount nocalhostAccount;
    private ServiceAccount serviceAccount;
    private String rawKubeConfig;
    private KubeConfig kubeConfig;

    @Setter
    private boolean active;
    @Setter
    private String info;

    public ClusterNode(String rawKubeConfig, KubeConfig kubeConfig) {
        this.rawKubeConfig = rawKubeConfig;
        this.kubeConfig = kubeConfig;
    }

    public ClusterNode(NocalhostAccount nocalhostAccount, ServiceAccount serviceAccount, String rawKubeConfig, KubeConfig kubeConfig) {
        this.nocalhostAccount = nocalhostAccount;
        this.serviceAccount = serviceAccount;
        this.rawKubeConfig = rawKubeConfig;
        this.kubeConfig = kubeConfig;
    }

    public String getName() {
        String name = kubeConfig.getClusters().get(0).getName();
        if (serviceAccount != null && StringUtils.isNotEmpty(serviceAccount.getClusterName())) {
            name = serviceAccount.getClusterName();
        }
        return name;
    }

    public String getAccountInfo() {
        if (nocalhostAccount != null) {
            return nocalhostAccount.getUsername() + " on " + nocalhostAccount.getServer();
        }
        return null;
    }

    public void updateFrom(ClusterNode o) {
        this.nocalhostAccount = o.nocalhostAccount;
        this.serviceAccount = o.serviceAccount;
        this.rawKubeConfig = o.rawKubeConfig;
        this.kubeConfig = o.kubeConfig;
        this.active = o.active;
        this.info = o.info;
    }

    @Override
    public boolean isLeaf() {
        return !active;
    }
}
