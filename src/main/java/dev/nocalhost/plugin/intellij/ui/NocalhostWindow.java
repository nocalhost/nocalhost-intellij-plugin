package dev.nocalhost.plugin.intellij.ui;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.ui.CheckedTreeNode;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.ui.tree.TreeUtil;

import java.awt.*;
import java.util.List;

import javax.swing.*;

import dev.nocalhost.plugin.intellij.api.NocalhostApi;
import dev.nocalhost.plugin.intellij.api.data.AuthData;
import dev.nocalhost.plugin.intellij.api.data.DevSpace;
import dev.nocalhost.plugin.intellij.settings.NocalhostSettings;
import dev.nocalhost.plugin.intellij.ui.action.RefreshAction;
import dev.nocalhost.plugin.intellij.ui.action.SettingsAction;
import dev.nocalhost.plugin.intellij.utils.CommonUtils;

public class NocalhostWindow {

    @Inject
    private SettingsAction settingsAction;
    @Inject
    private RefreshAction refreshAction;
    @Inject
    private NocalhostSettings nocalhostSettings;
    @Inject
    private NocalhostApi nocalhostApi;
    @Inject
    private CommonUtils commonUtils;
    @Inject
    private Logger log;

    public SimpleToolWindowPanel createToolWindowContent(final Project project) {
        SimpleToolWindowPanel panel = new SimpleToolWindowPanel(true, true);
        ActionToolbar toolbar = createToolbar(project);
        panel.setToolbar(toolbar.getComponent());
        JLabel label = new JLabel();
        AuthData authData = nocalhostSettings.getAuth();
        if (authData == null) {
            label.setText("Nocalhost");
        } else {
            label.setText(authData.getEmail());
        }
        panel.add(label, BorderLayout.NORTH);
        JPanel treePanel = new JPanel();
        panel.add(treePanel,BorderLayout.LINE_START);
        panel.add(addButtonPanel(project, label, treePanel), BorderLayout.SOUTH);
        panel.setVisible(true);
        return panel;
    }


    private Tree getDevSpaceTree(List<DevSpace> devSpaceList) {
        CheckedTreeNode devSpaceNode = new CheckedTreeNode("DevSpace");

        devSpaceList.forEach(devSpace -> {
            CheckedTreeNode devNode = new CheckedTreeNode(devSpace.getSpaceName());


            CheckedTreeNode workloads = new CheckedTreeNode("Workloads");
            CheckedTreeNode deployments = new CheckedTreeNode("Deployments");
            CheckedTreeNode statefulSets = new CheckedTreeNode("StatefulSets");
            CheckedTreeNode daemonSets = new CheckedTreeNode("DaemonSets");
            CheckedTreeNode jobs = new CheckedTreeNode("Jobs");
            CheckedTreeNode cronJobs = new CheckedTreeNode("CronJobs");
            CheckedTreeNode pods = new CheckedTreeNode("Pods");
            List<CheckedTreeNode> workloadsChildren = Lists.newArrayList(deployments, statefulSets, daemonSets, jobs, cronJobs, pods);
            TreeUtil.addChildrenTo(workloads, workloadsChildren);

            CheckedTreeNode networks = new CheckedTreeNode("Networks");
            CheckedTreeNode services = new CheckedTreeNode("Services");
            CheckedTreeNode endpoints = new CheckedTreeNode("Endpoints");
            CheckedTreeNode ingresses = new CheckedTreeNode("Ingresses");
            CheckedTreeNode networkPolicies = new CheckedTreeNode("Network Policies");
            List<CheckedTreeNode> networksChildren = Lists.newArrayList(services, endpoints, ingresses, networkPolicies);
            TreeUtil.addChildrenTo(networks, networksChildren);

            CheckedTreeNode configurations = new CheckedTreeNode("Configurations");
            CheckedTreeNode configMaps = new CheckedTreeNode("ConfigMaps");
            CheckedTreeNode secrets = new CheckedTreeNode("Secrets");
            CheckedTreeNode hpa = new CheckedTreeNode("HPA");
            CheckedTreeNode resourceQuotas = new CheckedTreeNode("Resource Quotas");
            CheckedTreeNode podDisruptionBudgets = new CheckedTreeNode("Pod Disruption Budgets");
            List<CheckedTreeNode> configurationsChildren = Lists.newArrayList(configMaps, secrets, hpa, resourceQuotas, podDisruptionBudgets);
            TreeUtil.addChildrenTo(configurations, configurationsChildren);

            CheckedTreeNode storage = new CheckedTreeNode("Storage");
            CheckedTreeNode persistentVolumes = new CheckedTreeNode("Persistent Volumes");
            CheckedTreeNode persistentVolumeClaims = new CheckedTreeNode("Persistent Volume Claims");
            CheckedTreeNode storageClasses = new CheckedTreeNode("Storage Classes");
            List<CheckedTreeNode> storageChildren = Lists.newArrayList(persistentVolumes, persistentVolumeClaims, storageClasses);
            TreeUtil.addChildrenTo(storage, storageChildren);

            List<CheckedTreeNode> devSpaceChildren = Lists.newArrayList(workloads, networks, configurations, storage);
            TreeUtil.addChildrenTo(devNode, devSpaceChildren);
            devSpaceNode.add(devNode);
        });

        return new Tree(devSpaceNode);
    }

    private JPanel addButtonPanel(final Project project, JLabel label, JPanel treePanel) {
        JPanel panel = new JPanel(new GridLayout(2, 1));


        JButton loginButton = new JButton("Login");
        JButton logoutButton = new JButton("Logout");

        panel.add(logoutButton);
        panel.add(loginButton);

        loginButton.addActionListener(e -> {
            final LoginDialog dialog = new LoginDialog(project, nocalhostSettings, commonUtils, log);
            dialog.show();
            if (dialog.isOK()) {
                AuthData _authData = nocalhostSettings.getAuth();
                if (_authData != null) {
                    label.setText(_authData.getEmail());
                    loginButton.setVisible(false);
                    logoutButton.setVisible(true);
                }
                List<DevSpace> devSpaceList = nocalhostApi.listDevSpace(_authData);
                treePanel.add(getDevSpaceTree(devSpaceList));
                treePanel.repaint();
            }
        });
        loginButton.setVisible(true);

        logoutButton.addActionListener(e -> {
            nocalhostSettings.clearAuth();
            label.setText("Nocalhost");
            treePanel.removeAll();
            loginButton.setVisible(true);
            logoutButton.setVisible(false);
        });
        logoutButton.setVisible(false);
        return panel;
    }

    private ActionToolbar createToolbar(final Project project) {
        DefaultActionGroup groupFromConfig = (DefaultActionGroup) ActionManager.getInstance().getAction("Nocalhost.Toolbar");
//        DefaultActionGroup group = new DefaultActionGroup(groupFromConfig); // copy required (otherwise config action group gets modified)
//        group.add(refreshAction);
//        group.add(settingsAction);
        return ActionManager.getInstance().createActionToolbar("Nocalhost.Toolbar", groupFromConfig, true);
    }
}
