package dev.nocalhost.plugin.intellij.ui;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.ui.CheckedTreeNode;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.ui.tree.TreeUtil;

import java.awt.*;
import java.util.List;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

import dev.nocalhost.plugin.intellij.api.NocalhostApi;
import dev.nocalhost.plugin.intellij.api.data.AuthData;
import dev.nocalhost.plugin.intellij.api.data.DevSpace;
import dev.nocalhost.plugin.intellij.settings.NocalhostSettings;
import dev.nocalhost.plugin.intellij.topic.NocalhostAccountChangedNotifier;
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

    Tree tree;
    JScrollPane treeView;
    JLabel label;
    SimpleToolWindowPanel panel;
    JButton loginButton;
    JButton logoutButton;

    public SimpleToolWindowPanel createToolWindowContent(final Project project) {
        panel = new SimpleToolWindowPanel(true, true);
        ActionToolbar toolbar = createToolbar(project);
        panel.setToolbar(toolbar.getComponent());
        label = new JBLabel();
        AuthData authData = nocalhostSettings.getAuth();
        if (authData == null) {
            label.setText("Nocalhost");
        } else {
            label.setText(authData.getEmail());
        }
        panel.add(label, BorderLayout.NORTH);
        loginButton = new JButton("Login");
        logoutButton = new JButton("Logout");


        buttonPanel(project, label, loginButton, logoutButton);
        final Application application = ApplicationManager.getApplication();
        application.getMessageBus().connect().subscribe(
                NocalhostAccountChangedNotifier.NOCALHOST_ACCOUNT_CHANGED_NOTIFIER_TOPIC,
                () -> buttonPanel(project, label, loginButton, logoutButton)
        );

        tree = new Tree();
//        treeView = new JScrollPane(tree, 20, 30);
//        treeView.add(tree);
        panel.add(tree, BorderLayout.LINE_START);
        panel.setVisible(true);
        return panel;
    }

    private void buttonPanel(Project project, JLabel label, JButton loginButton, JButton logoutButton) {


        if (nocalhostSettings.getAuth() == null) {
            panel.remove(logoutButton);
            loginButton.addActionListener(e -> {
                final LoginDialog dialog = new LoginDialog(project, nocalhostSettings, commonUtils, log);
                dialog.show();
                if (dialog.isOK()) {
                    List<DevSpace> devSpaceList = nocalhostApi.listDevSpace(nocalhostSettings.getAuth());
                    updateTree(devSpaceList);

                }
            });
            panel.add(loginButton, BorderLayout.SOUTH);
        } else {
            panel.remove(loginButton);
            label.setText(nocalhostSettings.getAuth().getEmail());
            DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
            DefaultMutableTreeNode root = (DefaultMutableTreeNode)model.getRoot();
            root.removeAllChildren();
            tree.removeAll();
            logoutButton.addActionListener(e -> {
                nocalhostSettings.clearAuth();
                label.setText("Nocalhost");
                final Application application = ApplicationManager.getApplication();
                NocalhostAccountChangedNotifier publisher = application.getMessageBus()
                                                                       .syncPublisher(NocalhostAccountChangedNotifier.NOCALHOST_ACCOUNT_CHANGED_NOTIFIER_TOPIC);
                publisher.action();
            });
            panel.add(logoutButton, BorderLayout.SOUTH);
        }

    }


    private void updateTree(List<DevSpace> devSpaceList) {
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

        DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
        DefaultMutableTreeNode root = (DefaultMutableTreeNode)model.getRoot();
        root.removeAllChildren();
        root.add(devSpaceNode);
        model.reload(root);
        tree.repaint();
        panel.repaint();
    }

    private ActionToolbar createToolbar(final Project project) {
        DefaultActionGroup groupFromConfig = (DefaultActionGroup) ActionManager.getInstance().getAction("Nocalhost.Toolbar");
//        DefaultActionGroup group = new DefaultActionGroup(groupFromConfig); // copy required (otherwise config action group gets modified)
//        group.add(refreshAction);
//        group.add(settingsAction);
        return ActionManager.getInstance().createActionToolbar("Nocalhost.Toolbar", groupFromConfig, true);
    }
}
