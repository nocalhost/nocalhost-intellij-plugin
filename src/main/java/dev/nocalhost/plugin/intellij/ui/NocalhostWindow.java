package dev.nocalhost.plugin.intellij.ui;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.ui.tree.TreeUtil;

import org.apache.commons.lang3.StringUtils;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.List;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import dev.nocalhost.plugin.intellij.api.NocalhostApi;
import dev.nocalhost.plugin.intellij.api.data.DevSpace;
import dev.nocalhost.plugin.intellij.commands.KubectlCommand;
import dev.nocalhost.plugin.intellij.commands.data.KubeResource;
import dev.nocalhost.plugin.intellij.commands.data.KubeResourceList;
import dev.nocalhost.plugin.intellij.settings.NocalhostSettings;
import dev.nocalhost.plugin.intellij.topic.DevSpaceListUpdatedNotifier;
import dev.nocalhost.plugin.intellij.topic.NocalhostAccountChangedNotifier;
import dev.nocalhost.plugin.intellij.ui.tree.AccountNode;
import dev.nocalhost.plugin.intellij.ui.tree.DevSpaceNode;
import dev.nocalhost.plugin.intellij.ui.tree.NodeRenderer;
import dev.nocalhost.plugin.intellij.ui.tree.PlainNode;
import dev.nocalhost.plugin.intellij.ui.tree.WorkloadNode;

public class NocalhostWindow {

    private final Project project;
    private final ToolWindow toolWindow;

    private JPanel panel;
    private Tree tree;
    private JButton loginButton;

    @Inject
    private Logger log;

    public NocalhostWindow(Project project, ToolWindow toolWindow) {
        this.project = project;
        this.toolWindow = toolWindow;

        final Application application = ApplicationManager.getApplication();
        application.getMessageBus().connect().subscribe(
                NocalhostAccountChangedNotifier.NOCALHOST_ACCOUNT_CHANGED_NOTIFIER_TOPIC,
                NocalhostWindow.this::toggleContent
        );
        application.getMessageBus().connect().subscribe(
                DevSpaceListUpdatedNotifier.DEV_SPACE_LIST_UPDATED_NOTIFIER_TOPIC,
                NocalhostWindow.this::updateTree
        );

        panel = new SimpleToolWindowPanel(true, true);
        loginButton = new JButton("Login");
        tree = new Tree();
        panel.add(tree, BorderLayout.LINE_START);
        panel.add(loginButton, BorderLayout.SOUTH);

        loginButton.addActionListener(e -> {
            new LoginDialog().showAndGet();
        });

        toggleContent();
    }

    private void toggleContent() {
        final NocalhostSettings nocalhostSettings = ServiceManager.getService(NocalhostSettings.class);

        String jwt = nocalhostSettings.getJwt();
        if (StringUtils.isNotBlank(jwt)) {
            setupTree();

            toolWindow.setTitleActions(Lists.newArrayList(
                    ActionManager.getInstance().getAction("Nocalhost.RefreshAction"),
                    ActionManager.getInstance().getAction("Nocalhost.LogoutAction")
            ));
            loginButton.setVisible(false);
            tree.setVisible(true);
        } else {
            toolWindow.setTitleActions(Lists.newArrayList());
            tree.setVisible(false);
            loginButton.setVisible(true);
        }
    }

    private void setupTree() {
        tree.setRootVisible(false);
        tree.setCellRenderer(new NodeRenderer());

        tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    TreePath treePath = tree.getPathForLocation(e.getX(), e.getY());
                    Object component = treePath.getLastPathComponent();
                    if (component instanceof DefaultMutableTreeNode) {
                        DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) component;
                        Object userObject = treeNode.getUserObject();
                        if (userObject instanceof DevSpaceNode) {
                            DevSpaceNode node = (DevSpaceNode) userObject;
                            new InstallDevSpaceDialog(node.getDevSpace()).showAndGet();
                        }
                    }
                }
            }
        });

        final NocalhostSettings nocalhostSettings = ServiceManager.getService(NocalhostSettings.class);

        DefaultMutableTreeNode root = new DefaultMutableTreeNode();
        root.add(new DefaultMutableTreeNode(new AccountNode(nocalhostSettings.getUserInfo().getName())));
        ((DefaultTreeModel) tree.getModel()).setRoot(root);

        final NocalhostApi nocalhostApi = ServiceManager.getService(NocalhostApi.class);
        try {
            List<DevSpace> devSpaces = nocalhostApi.listDevSpace();
            updateTree(devSpaces);
        } catch (IOException e) {
            // TODO: show balloon with error message
            e.printStackTrace();
        }
    }

    private void updateTree(List<DevSpace> devSpaces) {
        final NocalhostSettings nocalhostSettings = ServiceManager.getService(NocalhostSettings.class);
        final KubectlCommand kubectlCommand = ServiceManager.getService(KubectlCommand.class);

        DefaultMutableTreeNode root = new DefaultMutableTreeNode();
        root.add(new DefaultMutableTreeNode(new AccountNode(nocalhostSettings.getUserInfo().getName())));
        for (DevSpace devSpace : devSpaces) {
            DefaultMutableTreeNode node = new DefaultMutableTreeNode(new DevSpaceNode(devSpace));
            root.add(node);
            if (devSpace.getInstallStatus() == 0) {
                continue;
            }

            DefaultMutableTreeNode workloadsNode = new DefaultMutableTreeNode(new PlainNode("Workloads"));
            node.add(workloadsNode);
            DefaultMutableTreeNode deploymentsNode = new DefaultMutableTreeNode(new PlainNode("Deployments"));
            workloadsNode.add(deploymentsNode);

            try {
                KubeResourceList list = kubectlCommand.getResourceList("deployments", devSpace);
                for (KubeResource resource : list.getItems()) {
                    String name = resource.getMetadata().getName();
                    WorkloadNode.Status status = WorkloadNode.Status.UNKNOWN;
                    boolean available = false;
                    boolean progressing = false;
                    for (KubeResource.Status.Condition condition : resource.getStatus().getConditions()) {
                        if (StringUtils.equals(condition.getType(), "Available") && StringUtils.equals(condition.getStatus(), "True")) {
                            status = WorkloadNode.Status.RUNNING;
                            available = true;
                        } else if (StringUtils.equals(condition.getType(), "Progressing") && StringUtils.equals(condition.getStatus(), "True")) {
                            progressing = true;
                        }
                    }
                    if (progressing && !available) {
                        status = WorkloadNode.Status.STARTING;
                    }
                    deploymentsNode.add(new DefaultMutableTreeNode(new WorkloadNode(name, status)));

                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            workloadsNode.add(new DefaultMutableTreeNode(new PlainNode("StatefulSets")));
            workloadsNode.add(new DefaultMutableTreeNode(new PlainNode("DaemonSets")));
            workloadsNode.add(new DefaultMutableTreeNode(new PlainNode("Jobs")));
            workloadsNode.add(new DefaultMutableTreeNode(new PlainNode("CronJobs")));
            workloadsNode.add(new DefaultMutableTreeNode(new PlainNode("Pods")));


            DefaultMutableTreeNode networks = new DefaultMutableTreeNode(new PlainNode("Networks"));
            DefaultMutableTreeNode services = new DefaultMutableTreeNode(new PlainNode("Services"));
            DefaultMutableTreeNode endpoints = new DefaultMutableTreeNode(new PlainNode("Endpoints"));
            DefaultMutableTreeNode ingresses = new DefaultMutableTreeNode(new PlainNode("Ingresses"));
            DefaultMutableTreeNode networkPolicies = new DefaultMutableTreeNode(new PlainNode("Network Policies"));
            List<DefaultMutableTreeNode> networksChildren = Lists.newArrayList(services, endpoints, ingresses, networkPolicies);
            TreeUtil.addChildrenTo(networks, networksChildren);
            node.add(networks);

            DefaultMutableTreeNode configurations = new DefaultMutableTreeNode(new PlainNode("Configurations"));
            DefaultMutableTreeNode configMaps = new DefaultMutableTreeNode(new PlainNode("ConfigMaps"));
            DefaultMutableTreeNode secrets = new DefaultMutableTreeNode(new PlainNode("Secrets"));
            DefaultMutableTreeNode hpa = new DefaultMutableTreeNode(new PlainNode("HPA"));
            DefaultMutableTreeNode resourceQuotas = new DefaultMutableTreeNode(new PlainNode("Resource Quotas"));
            DefaultMutableTreeNode podDisruptionBudgets = new DefaultMutableTreeNode(new PlainNode("Pod Disruption Budgets"));
            List<DefaultMutableTreeNode> configurationsChildren = Lists.newArrayList(configMaps, secrets, hpa, resourceQuotas, podDisruptionBudgets);
            TreeUtil.addChildrenTo(configurations, configurationsChildren);
            node.add(configurations);


            DefaultMutableTreeNode storage = new DefaultMutableTreeNode(new PlainNode("Storage"));
            DefaultMutableTreeNode persistentVolumes = new DefaultMutableTreeNode(new PlainNode("Persistent Volumes"));
            DefaultMutableTreeNode persistentVolumeClaims = new DefaultMutableTreeNode(new PlainNode("Persistent Volume Claims"));
            DefaultMutableTreeNode storageClasses = new DefaultMutableTreeNode(new PlainNode("Storage Classes"));
            List<DefaultMutableTreeNode> storageChildren = Lists.newArrayList(persistentVolumes, persistentVolumeClaims, storageClasses);
            TreeUtil.addChildrenTo(storage, storageChildren);
            node.add(storage);

        }
        ((DefaultTreeModel) tree.getModel()).setRoot(root);
    }

    public JPanel getPanel() {
        return panel;
    }
}
