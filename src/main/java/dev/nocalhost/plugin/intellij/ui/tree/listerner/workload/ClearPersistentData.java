package dev.nocalhost.plugin.intellij.ui.tree.listerner.workload;

import com.intellij.openapi.components.ServiceManager;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.List;

import dev.nocalhost.plugin.intellij.commands.NhctlCommand;
import dev.nocalhost.plugin.intellij.commands.data.NhctlListPVCOptions;
import dev.nocalhost.plugin.intellij.commands.data.NhctlPVCItem;
import dev.nocalhost.plugin.intellij.ui.ClearPersistentDataDialog;
import dev.nocalhost.plugin.intellij.ui.tree.node.ResourceNode;
import dev.nocalhost.plugin.intellij.utils.KubeConfigUtil;

public class ClearPersistentData implements ActionListener {
    private ResourceNode node;

    public ClearPersistentData(ResourceNode node) {
        this.node = node;
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        final NhctlCommand nhctlCommand = ServiceManager.getService(NhctlCommand.class);

        NhctlListPVCOptions opts = new NhctlListPVCOptions();
        opts.setApp(node.devSpace().getContext().getApplicationName());
        opts.setSvc(node.getNhctlDescribeService().getRawConfig().getName());
        opts.setKubeconfig(KubeConfigUtil.kubeConfigPath(node.devSpace()).toString());
        try {
            List<NhctlPVCItem> nhctlPVCItems = nhctlCommand.listPVC(opts);
            new ClearPersistentDataDialog(node.devSpace(), nhctlPVCItems, false).showAndGet();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
