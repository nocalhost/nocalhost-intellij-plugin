package dev.nocalhost.plugin.intellij.ui.tree.listerner.devspace;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.List;

import dev.nocalhost.plugin.intellij.commands.NhctlCommand;
import dev.nocalhost.plugin.intellij.commands.data.NhctlListPVCOptions;
import dev.nocalhost.plugin.intellij.commands.data.NhctlPVCItem;
import dev.nocalhost.plugin.intellij.ui.ClearPersistentDataDialog;
import dev.nocalhost.plugin.intellij.ui.tree.node.DevSpaceNode;
import dev.nocalhost.plugin.intellij.utils.KubeConfigUtil;

public class ClearPersistentData implements ActionListener {
    private static final Logger LOG = Logger.getInstance(ClearPersistentData.class);

    private DevSpaceNode node;

    public ClearPersistentData(DevSpaceNode node) {
        this.node = node;
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        final NhctlCommand nhctlCommand = ServiceManager.getService(NhctlCommand.class);

        NhctlListPVCOptions opts = new NhctlListPVCOptions();
        opts.setApp(node.getDevSpace().getContext().getApplicationName());
        opts.setKubeconfig(KubeConfigUtil.kubeConfigPath(node.getDevSpace()).toString());
        try {
            List<NhctlPVCItem> nhctlPVCItems = nhctlCommand.listPVC(opts);
            new ClearPersistentDataDialog(node.getDevSpace(), nhctlPVCItems, true).showAndGet();
        } catch (IOException | InterruptedException e) {
            LOG.error("error occurred while listing pvc items", e);
        }
    }
}