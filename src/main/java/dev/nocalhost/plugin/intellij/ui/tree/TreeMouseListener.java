package dev.nocalhost.plugin.intellij.ui.tree;


import com.intellij.ide.impl.OpenProjectTask;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ex.ProjectManagerEx;
import com.intellij.openapi.ui.JBMenuItem;
import com.intellij.openapi.ui.JBPopupMenu;
import com.intellij.openapi.ui.MessageDialogBuilder;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.treeStructure.Tree;

import org.jetbrains.annotations.NotNull;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.nio.file.Path;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import dev.nocalhost.plugin.intellij.api.NocalhostApi;
import dev.nocalhost.plugin.intellij.api.data.DevModeService;
import dev.nocalhost.plugin.intellij.api.data.DevSpace;
import dev.nocalhost.plugin.intellij.commands.NhctlCommand;
import dev.nocalhost.plugin.intellij.commands.data.NhctlDescribeOptions;
import dev.nocalhost.plugin.intellij.commands.data.NhctlDescribeResult;
import dev.nocalhost.plugin.intellij.commands.data.NhctlDevEndOptions;
import dev.nocalhost.plugin.intellij.commands.data.NhctlUninstallOptions;
import dev.nocalhost.plugin.intellij.settings.NocalhostSettings;
import dev.nocalhost.plugin.intellij.topic.DevSpaceListUpdatedNotifier;
import dev.nocalhost.plugin.intellij.ui.InstallDevSpaceDialog;
import dev.nocalhost.plugin.intellij.utils.KubeConfigUtil;

public class TreeMouseListener extends MouseAdapter {
    private Project project;
    private Tree tree;

    public TreeMouseListener(Tree tree, Project project) {
        this.tree = tree;
        this.project = project;
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

                            if (!MessageDialogBuilder.yesNo("Uninstall application: " + appName, "").guessWindowAndAsk()) {
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
                    return;
                }

                if (userObject instanceof WorkloadNode) {
                    WorkloadNode node = (WorkloadNode) userObject;

                    JBPopupMenu menu = new JBPopupMenu();

                    final NhctlCommand nhctlCommand = ServiceManager.getService(NhctlCommand.class);
                    final NhctlDescribeOptions opts = new NhctlDescribeOptions();
                    opts.setDeployment(node.getName());
                    try {
                        final NhctlDescribeResult describeResult = nhctlCommand.describe(node.getDevSpace().getContext().getApplicationName(), opts);
                        if (!describeResult.isDeveloping()) {
                            JBMenuItem item = new JBMenuItem("Start Develop");
                            item.addActionListener(e12 -> {
                                int exitCode = MessageDialogBuilder.yesNoCancel("To start develop, you must specify source code directory.", "")
                                        .yesText("Clone from Git Repo")
                                        .noText("Open local directly")
                                        .guessWindowAndAsk();
                                switch (exitCode) {
                                    case Messages.YES:
                                        // TODO: Git.getInstance().clone(...)
                                        break;
                                    case Messages.NO:
                                        final FileChooserDescriptor dirChooser = FileChooserDescriptorFactory.createSingleFolderDescriptor();
                                        dirChooser.setShowFileSystemRoots(true);
                                        FileChooser.chooseFiles(dirChooser, null, null, paths -> {
                                            Path bashPath = paths.get(0).toNioPath();

                                            final NocalhostSettings nocalhostSettings = ServiceManager.getService(NocalhostSettings.class);
                                            nocalhostSettings.getDevModeProjectBasePath2Service().put(
                                                    bashPath.toString(),
                                                    new DevModeService(node.getDevSpace().getId(), node.getDevSpace().getDevSpaceId(), node.getName())
                                            );

                                            ProjectManagerEx.getInstanceEx().openProject(bashPath, new OpenProjectTask());
                                        });
                                        break;
                                    default:
                                        // Empty
                                }
                            });
                            menu.add(item);
                        } else {
                            JBMenuItem item = new JBMenuItem("End Develop");
                            item.addActionListener(e1 -> {
                                ProgressManager.getInstance().run(new Task.Backgroundable(null, "Ending DevMode", false) {
                                    @Override
                                    public void run(@NotNull ProgressIndicator indicator) {
                                        final NhctlCommand nhctlCommand = ServiceManager.getService(NhctlCommand.class);

                                        NhctlDevEndOptions opts = new NhctlDevEndOptions();
                                        opts.setDeployment(node.getName());
                                        opts.setKubeconfig(KubeConfigUtil.kubeConfigPath(node.getDevSpace()).toString());

                                        try {
                                            nhctlCommand.devEnd(node.getDevSpace().getContext().getApplicationName(), opts);

                                            Notifications.Bus.notify(new Notification("Nocalhost.Notification", "DevMode ended", "", NotificationType.INFORMATION), project);

                                        } catch (IOException ioException) {
                                            ioException.printStackTrace();
                                        } catch (InterruptedException interruptedException) {
                                            interruptedException.printStackTrace();
                                        }
                                    }
                                });
                            });
                            menu.add(item);
                        }
                    } catch (IOException ioException) {
                        throw new RuntimeException(ioException);
                    } catch (InterruptedException interruptedException) {
                        throw new RuntimeException(interruptedException);
                    }

                    menu.add(new JBMenuItem("Config"));
                    menu.addSeparator();
                    menu.add(new JBMenuItem("Clear persistent data"));
                    menu.addSeparator();
                    menu.add(new JBMenuItem("Logs"));
                    menu.add(new JBMenuItem("Port Forward"));
                    menu.add(new JBMenuItem("Reset"));
                    menu.add(new JBMenuItem("Terminal"));
                    JBPopupMenu.showByEvent(e, menu);
                    return;
                }
            }
        }
    }
}
