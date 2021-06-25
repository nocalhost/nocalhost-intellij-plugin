package dev.nocalhost.plugin.intellij.ui.tree.node;

import org.apache.commons.lang3.StringUtils;

import javax.swing.tree.DefaultMutableTreeNode;

import dev.nocalhost.plugin.intellij.api.data.ServiceAccount;
import dev.nocalhost.plugin.intellij.data.kubeconfig.KubeConfig;
import dev.nocalhost.plugin.intellij.settings.data.NocalhostAccount;
import lombok.Getter;

@Getter
public class ClusterNode extends DefaultMutableTreeNode {
    private NocalhostAccount nocalhostAccount;
    private ServiceAccount serviceAccount;
    private String rawKubeConfig;
    private KubeConfig kubeConfig;

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
        if (nocalhostAccount != null) {
            name += " [" + nocalhostAccount.getUsername() + " on " + nocalhostAccount.getServer() + "]";
        }
        return name;
    }

    public void updateFrom(ClusterNode o) {
        this.nocalhostAccount = o.nocalhostAccount;
        this.serviceAccount = o.serviceAccount;
        this.rawKubeConfig = o.rawKubeConfig;
        this.kubeConfig = o.kubeConfig;
    }

    @Override
    public boolean isLeaf() {
        return false;
    }
}
