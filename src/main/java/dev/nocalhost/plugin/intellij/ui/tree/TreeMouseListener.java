package dev.nocalhost.plugin.intellij.ui.tree;


import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.JBMenuItem;
import com.intellij.openapi.ui.JBPopupMenu;
import com.intellij.ui.treeStructure.Tree;

import org.apache.commons.lang3.EnumUtils;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.tree.TreePath;

import dev.nocalhost.plugin.intellij.commands.data.KubeResourceType;
import dev.nocalhost.plugin.intellij.ui.InstallDevSpaceDialog;
import dev.nocalhost.plugin.intellij.ui.tree.listerner.devspace.Install;
import dev.nocalhost.plugin.intellij.ui.tree.listerner.devspace.Uninstall;
import dev.nocalhost.plugin.intellij.ui.tree.listerner.workload.Config;
import dev.nocalhost.plugin.intellij.ui.tree.listerner.workload.EndDevelop;
import dev.nocalhost.plugin.intellij.ui.tree.listerner.workload.Logs;
import dev.nocalhost.plugin.intellij.ui.tree.listerner.workload.Reset;
import dev.nocalhost.plugin.intellij.ui.tree.listerner.workload.StartDevelop;
import dev.nocalhost.plugin.intellij.ui.tree.listerner.workload.Terminal;
import dev.nocalhost.plugin.intellij.ui.tree.node.DevSpaceNode;
import dev.nocalhost.plugin.intellij.ui.tree.node.ResourceNode;

import static dev.nocalhost.plugin.intellij.commands.data.KubeResourceType.Deployment;
import static dev.nocalhost.plugin.intellij.commands.data.KubeResourceType.Job;
import static dev.nocalhost.plugin.intellij.commands.data.KubeResourceType.Pod;

public class TreeMouseListener extends MouseAdapter {
    private final Project project;
    private final Tree tree;

    public TreeMouseListener(Tree tree, Project project) {
        this.tree = tree;
        this.project = project;
    }

    private TreePath getPath(MouseEvent e) {
        TreePath treePath = tree.getClosestPathForLocation(e.getX(), e.getY());
        if (treePath == null) {
            treePath = tree.getPathForLocation(e.getX(), e.getY());
        }
        return treePath;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1) {
            TreePath treePath = getPath(e);
            if (treePath == null) {
                return;
            }
            Object object = treePath.getLastPathComponent();
            if (object instanceof DevSpaceNode) {
                DevSpaceNode devSpaceNode = (DevSpaceNode) object;
                if (devSpaceNode.getDevSpace().getInstallStatus() == 1) {
                    return;
                }
                new InstallDevSpaceDialog(devSpaceNode.getDevSpace()).showAndGet();
            }
        }
    }

    @Override
    public void mouseReleased(MouseEvent mouseEvent) {
        if (mouseEvent.getButton() == MouseEvent.BUTTON3) {
            TreePath treePath = getPath(mouseEvent);
            if (treePath == null) {
                return;
            }
            Object object = treePath.getLastPathComponent();

            if (object instanceof DevSpaceNode) {
                DevSpaceNode devSpaceNode = (DevSpaceNode) object;

                JBPopupMenu menu = new JBPopupMenu();
                if (devSpaceNode.getDevSpace().getInstallStatus() == 1) {
                    JBMenuItem item = new JBMenuItem("Uninstall App");
                    item.addActionListener(new Uninstall(devSpaceNode));
                    menu.add(item);

                    menu.addSeparator();
                    menu.add(new JBMenuItem("Clear persistent data"));
                    menu.add(new JBMenuItem("Reset"));

                    menu.addSeparator();
                    menu.add(new JBMenuItem("Load resource"));
                } else if (devSpaceNode.getDevSpace().getInstallStatus() == 0) {
                    JBMenuItem item = new JBMenuItem("Install App");
                    item.addActionListener(new Install(devSpaceNode));
                    menu.add(item);

                    menu.addSeparator();
                    menu.add(new JBMenuItem("Clear persistent data"));
                    menu.add(new JBMenuItem("Reset"));
                }

                JBPopupMenu.showByEvent(mouseEvent, menu);
                return;
            }

            if (object instanceof ResourceNode) {
                ResourceNode resourceNode = (ResourceNode) object;
                JBPopupMenu menu = new JBPopupMenu();
                JBMenuItem clearPersistentDataItem = new JBMenuItem("Clear persistent data");
                JBMenuItem configItem = new JBMenuItem("Config");
                JBMenuItem logsItem = new JBMenuItem("Logs");
                JBMenuItem portForwardItem = new JBMenuItem("Port Forward");
                JBMenuItem resetItem = new JBMenuItem("Reset");
                JBMenuItem terminalItem = new JBMenuItem("Terminal");

                String kind = resourceNode.getKubeResource().getKind().toLowerCase();
                KubeResourceType type = EnumUtils.getEnumIgnoreCase(KubeResourceType.class, kind);
                switch (type) {
                    case Deployment:
                        JBMenuItem devItem;
                        if (!resourceNode.getNhctlDescribeService().isDeveloping()) {
                            devItem = new JBMenuItem("Start Develop");
                            devItem.addActionListener(new StartDevelop(resourceNode));
                        } else {
                            devItem = new JBMenuItem("End Develop");
                            devItem.addActionListener(new EndDevelop(resourceNode, project));
                        }

                        menu.add(devItem);

                        configItem.addActionListener(new Config(resourceNode, project));
                        logsItem.addActionListener(new Logs(resourceNode, Deployment, project));
                        resetItem.addActionListener(new Reset(resourceNode, project));
                        terminalItem.addActionListener(new Terminal(resourceNode, Deployment, project));

                        menu.add(configItem);
                        menu.addSeparator();
                        menu.add(clearPersistentDataItem);
                        menu.addSeparator();
                        menu.add(logsItem);
                        menu.add(portForwardItem);
                        menu.add(resetItem);
                        menu.add(terminalItem);
                        JBPopupMenu.showByEvent(mouseEvent, menu);
                        break;
                    case Daemonset:
                        break;
                    case Statefulset:
                        break;
                    case Job:
                        logsItem.addActionListener(new Logs(resourceNode, Job, project));

                        menu.add(logsItem);
                        menu.add(portForwardItem);
                        JBPopupMenu.showByEvent(mouseEvent, menu);
                        break;
                    case CronJobs:
                        break;
                    case Pod:
                        logsItem.addActionListener(new Logs(resourceNode, Pod, project));
                        terminalItem.addActionListener(new Terminal(resourceNode, Deployment, project));

                        menu.add(logsItem);
                        menu.add(portForwardItem);
                        menu.add(terminalItem);
                        JBPopupMenu.showByEvent(mouseEvent, menu);
                        break;
                    default:
                        throw new IllegalStateException("Unexpected value: " + kind);
                }
            }
        }
    }
}
