package dev.nocalhost.plugin.intellij.ui.tree;


import com.intellij.CommonBundle;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.ui.JBMenuItem;
import com.intellij.openapi.ui.JBPopupMenu;
import com.intellij.openapi.ui.MessageDialogBuilder;
import com.intellij.ui.treeStructure.Tree;

import org.jetbrains.annotations.NotNull;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import dev.nocalhost.plugin.intellij.api.NocalhostApi;
import dev.nocalhost.plugin.intellij.api.data.DevSpace;
import dev.nocalhost.plugin.intellij.commands.NhctlCommand;
import dev.nocalhost.plugin.intellij.commands.data.NhctlUninstallOptions;
import dev.nocalhost.plugin.intellij.topic.DevSpaceListUpdatedNotifier;
import dev.nocalhost.plugin.intellij.ui.InstallDevSpaceDialog;
import dev.nocalhost.plugin.intellij.utils.KubeConfigUtil;

public class TreeMouseListener extends MouseAdapter {
    private Tree tree;

    public TreeMouseListener(Tree tree) {
        this.tree = tree;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1) {
            TreePath treePath = tree.getPathForLocation(e.getX(), e.getY());
            if (treePath == null) {
                return;
            }
            Object component = treePath.getLastPathComponent();
            if (component instanceof DefaultMutableTreeNode) {
                DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) component;
                if (treeNode.getChildCount() > 0) {
                    return;
                }
                Object userObject = treeNode.getUserObject();
                if (userObject instanceof DevSpaceNode) {
                    DevSpaceNode node = (DevSpaceNode) userObject;
                    new InstallDevSpaceDialog(node.getDevSpace()).showAndGet();
                }
            }
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON3) {
            TreePath treePath = tree.getPathForLocation(e.getX(), e.getY());
            if (treePath == null) {
                return;
            }
            Object component = treePath.getLastPathComponent();
            if (component instanceof DefaultMutableTreeNode) {
                DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) component;
                Object userObject = treeNode.getUserObject();
                if (userObject instanceof DevSpaceNode) {
                    DevSpaceNode node = (DevSpaceNode) userObject;
                    JBPopupMenu menu = new JBPopupMenu();
                    if (node.getDevSpace().getInstallStatus() == 1) {
                        JBMenuItem item = new JBMenuItem("Uninstall App");
                        item.addActionListener(e1 -> {
                            DevSpace.Context context = node.getDevSpace().getContext();
                            String appName = context.getApplicationName();

                            MessageDialogBuilder.YesNo confirmExitDialog = MessageDialogBuilder.yesNo("Uninstall application: " + appName, "")
                                                                                               .yesText(CommonBundle.getYesButtonText())
                                                                                               .noText(CommonBundle.getCancelButtonText());
                            if (!confirmExitDialog.guessWindowAndAsk()) {
                                return;
                            }

                            ProgressManager.getInstance().run(new Task.Backgroundable(null, "Uninstalling application: " + appName, false) {
                                @Override
                                public void run(@NotNull ProgressIndicator indicator) {
                                    final NhctlCommand nhctlCommand = ServiceManager.getService(NhctlCommand.class);

                                    NhctlUninstallOptions opts = new NhctlUninstallOptions();
                                    opts.setForce(true);
                                    opts.setKubeconfig(KubeConfigUtil.kubeConfigPath(node.getDevSpace()).toString());
                                    try {
                                        nhctlCommand.uninstall(appName, opts);

                                        final NocalhostApi nocalhostApi = ServiceManager.getService(NocalhostApi.class);
                                        nocalhostApi.syncInstallStatus(node.getDevSpace(), 0);

                                        final Application application = ApplicationManager.getApplication();
                                        DevSpaceListUpdatedNotifier publisher = application.getMessageBus()
                                                                                           .syncPublisher(DevSpaceListUpdatedNotifier.DEV_SPACE_LIST_UPDATED_NOTIFIER_TOPIC);
                                        publisher.action();

                                        Notifications.Bus.notify(new Notification("Nocalhost.Notification", "Application " + appName + " uninstalled", "", NotificationType.INFORMATION));
                                    } catch (InterruptedException interruptedException) {
                                        interruptedException.printStackTrace();
                                    } catch (IOException ioException) {
                                        ioException.printStackTrace();
                                    }
                                }
                            });


                        });
                        menu.add(item);

                        menu.addSeparator();

                        menu.add(new JBMenuItem("Clear persistent data"));
                        menu.add(new JBMenuItem("Reset"));

                        menu.addSeparator();

                        menu.add(new JBMenuItem("Load resource"));
                    } else if (node.getDevSpace().getInstallStatus() == 0) {
                        JBMenuItem item = new JBMenuItem("Install App");
                        item.addActionListener(e1 -> {
                            new InstallDevSpaceDialog(node.getDevSpace()).showAndGet();
                        });
                        menu.add(item);

                        menu.addSeparator();

                        menu.add(new JBMenuItem("Clear persistent data"));
                        menu.add(new JBMenuItem("Reset"));
                    }

                    JBPopupMenu.showByEvent(e, menu);
                }
            }
        }
    }
}
