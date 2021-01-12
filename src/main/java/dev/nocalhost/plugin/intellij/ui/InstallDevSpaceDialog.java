package dev.nocalhost.plugin.intellij.ui;

import com.google.common.collect.Lists;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.ui.DialogWrapper;

import org.jetbrains.annotations.Nullable;

import java.io.IOException;

import javax.swing.*;

import dev.nocalhost.plugin.intellij.api.data.DevSpace;
import dev.nocalhost.plugin.intellij.commands.NhctlCommand;
import dev.nocalhost.plugin.intellij.commands.data.NhctlInstallOptions;
import dev.nocalhost.plugin.intellij.utils.KubeConfigUtil;

public class InstallDevSpaceDialog extends DialogWrapper {
    private JPanel dialogPanel;
    private JRadioButton defaultBranchRadioButton;
    private JRadioButton specifyOneRadioButton;
    private JTextField customBranchField;

    public InstallDevSpaceDialog(DevSpace devSpace) {
        super(true);
        init();
        setTitle("Install DevSpace: " + devSpace.getSpaceName());

        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(defaultBranchRadioButton);
        buttonGroup.add(specifyOneRadioButton);

        final NhctlCommand nhctlCommand = ServiceManager.getService(NhctlCommand.class);

        NhctlInstallOptions opts = new NhctlInstallOptions();
        opts.setGitUrl("https://github.com/nocalhost/bookinfo.git");
        opts.setType("rawManifest");
        opts.setResourcesPath(Lists.newArrayList("manifest/templates", "manifest/templates3"));
        opts.setKubeconfig(KubeConfigUtil.kubeConfigPath(devSpace).toString());

        try {
            nhctlCommand.install("bookinfo", opts);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return dialogPanel;
    }
}
