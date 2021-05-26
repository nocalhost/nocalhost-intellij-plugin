package dev.nocalhost.plugin.intellij.ui;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageDialogBuilder;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.util.Consumer;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.event.MouseEvent;
import java.io.IOException;
import java.nio.file.Path;

import javax.swing.*;

import dev.nocalhost.plugin.intellij.commands.NhctlCommand;
import dev.nocalhost.plugin.intellij.commands.data.NhctlDescribeOptions;
import dev.nocalhost.plugin.intellij.commands.data.NhctlDescribeService;
import dev.nocalhost.plugin.intellij.commands.data.NhctlSyncResumeOptions;
import dev.nocalhost.plugin.intellij.commands.data.NhctlSyncStatus;
import dev.nocalhost.plugin.intellij.commands.data.NhctlSyncStatusOptions;
import dev.nocalhost.plugin.intellij.exception.NocalhostExecuteCmdException;
import dev.nocalhost.plugin.intellij.settings.NocalhostProjectSettings;
import dev.nocalhost.plugin.intellij.settings.data.ServiceProjectPath;
import dev.nocalhost.plugin.intellij.utils.DataUtils;
import dev.nocalhost.plugin.intellij.utils.KubeConfigUtil;

public class SyncStatusPresentation implements StatusBarWidget.MultipleTextValuesPresentation, StatusBarWidget.Multiframe {

    private static final Logger LOG = Logger.getInstance(SyncStatusPresentation.class);

    private final StatusBar statusBar;
    private final Project project;
    private final Disposable widget;

    private NhctlSyncStatus nhctlSyncStatus = getNhctlSyncStatus();

    private NhctlSyncStatus getNhctlSyncStatus() {
        if (project == null) {
            return null;
        }
        final NhctlCommand nhctlCommand = ServiceManager.getService(NhctlCommand.class);
        final NocalhostProjectSettings nocalhostProjectSettings = project.getService(NocalhostProjectSettings.class);

        ServiceProjectPath devModeService = nocalhostProjectSettings.getDevModeService();
        if (devModeService == null || devModeService.getRawKubeConfig() == null) {
            return null;
        }

        Path kubeConfigPath = KubeConfigUtil.kubeConfigPath(devModeService.getRawKubeConfig());

        String status = null;
        try {
            NhctlDescribeOptions nhctlDescribeOptions = new NhctlDescribeOptions(kubeConfigPath,
                    devModeService.getNamespace());
            nhctlDescribeOptions.setDeployment(devModeService.getServiceName());
            nhctlDescribeOptions.setType(devModeService.getServiceType());
            NhctlDescribeService nhctlDescribeService = nhctlCommand.describe(
                    devModeService.getApplicationName(),
                    nhctlDescribeOptions,
                    NhctlDescribeService.class
            );
            if (nhctlDescribeService.isDeveloping()) {
                if (!nhctlDescribeService.isPossess()) {
                    return null;
                }
            } else {
                return null;
            }

            NhctlSyncStatusOptions nhctlSyncStatusOptions = new NhctlSyncStatusOptions(kubeConfigPath, devModeService.getNamespace());
            nhctlSyncStatusOptions.setDeployment(devModeService.getServiceName());
            status = nhctlCommand.syncStatus(devModeService.getApplicationName(), nhctlSyncStatusOptions);
        } catch (InterruptedException | IOException e) {
            LOG.error("error occurred while get sync status ", e);
        } catch (NocalhostExecuteCmdException e) {
            if (!StringUtils.contains(e.getMessage(), "not found")) {
                LOG.error("Fail to get sync status", e);
            }
        }
        nhctlSyncStatus = DataUtils.GSON.fromJson(status, NhctlSyncStatus.class);
        return nhctlSyncStatus;
    }

    public SyncStatusPresentation(StatusBar statusBar, Project project, Disposable widget) {
        this.statusBar = statusBar;
        this.project = project;
        this.widget = widget;
    }

    @Override
    public StatusBarWidget copy() {
        return new SyncStatusWidget(project);
    }

    @Override
    public @NonNls
    @NotNull String ID() {
        return "Nocalhost Sync Status";
    }

    @Override
    public void install(@NotNull StatusBar statusBar) {
    }

    @Override
    public void dispose() {
        Disposer.dispose(widget);
    }

    @Override
    public @Nullable String getTooltipText() {
        if (nhctlSyncStatus != null) {
            if (StringUtils.isNoneBlank(nhctlSyncStatus.getOutOfSync())) {
                return nhctlSyncStatus.getOutOfSync();
            }
            return nhctlSyncStatus.getTips();
        }
        return "";
    }

    @Override
    public @Nullable Consumer<MouseEvent> getClickConsumer() {
        return mouseEvent -> {
            statusBar.updateWidget("Nocalhost Sync Status");
        };
    }

    @Override
    public @Nullable("null means the widget is unable to show the popup") ListPopup getPopupStep() {
        if (nhctlSyncStatus != null && nhctlSyncStatus.getStatus().equalsIgnoreCase("disconnected")) {
            int exitCode = MessageDialogBuilder.yesNoCancel("Sync resume", "do you want to resume file sync?")
                    .guessWindowAndAsk();
            switch (exitCode) {
                case Messages.YES: {
                    final NhctlCommand nhctlCommand = ServiceManager.getService(NhctlCommand.class);
                    final NocalhostProjectSettings nocalhostProjectSettings = project.getService(NocalhostProjectSettings.class);

                    ServiceProjectPath devModeService = nocalhostProjectSettings.getDevModeService();
                    if (devModeService == null || devModeService.getRawKubeConfig() == null) {
                        return null;
                    }

                    Path kubeConfigPath = KubeConfigUtil.kubeConfigPath(devModeService.getRawKubeConfig());

                    try {
                        NhctlDescribeOptions nhctlDescribeOptions = new NhctlDescribeOptions(kubeConfigPath,
                                devModeService.getNamespace());
                        nhctlDescribeOptions.setDeployment(devModeService.getServiceName());
                        nhctlDescribeOptions.setType(devModeService.getServiceType());
                        NhctlDescribeService nhctlDescribeService = nhctlCommand.describe(
                                devModeService.getApplicationName(),
                                nhctlDescribeOptions,
                                NhctlDescribeService.class
                        );
                        if (nhctlDescribeService.isDeveloping()) {
                            if (!nhctlDescribeService.isPossess()) {
                                return null;
                            }
                        } else {
                            return null;
                        }

                        NhctlSyncResumeOptions nhctlSyncResumeOptions = new NhctlSyncResumeOptions(kubeConfigPath, devModeService.getNamespace());
                        nhctlSyncResumeOptions.setDeployment(devModeService.getServiceName());
                        nhctlCommand.syncResume(devModeService.getApplicationName(), nhctlSyncResumeOptions);
                    } catch (InterruptedException | NocalhostExecuteCmdException | IOException e) {
                        LOG.error("error occurred while sync resume ", e);
                    }
                }
                case Messages.NO: {
                    break;
                }
                default:
            }
        }
        if (nhctlSyncStatus != null && StringUtils.isNoneBlank(nhctlSyncStatus.getOutOfSync())) {
            int exitCode = MessageDialogBuilder.yesNoCancel("Sync warning", "Override the remote changes according to the local folders?")
                    .guessWindowAndAsk();
            switch (exitCode) {
                case Messages.YES: {
                    final NhctlCommand nhctlCommand = ServiceManager.getService(NhctlCommand.class);
                    final NocalhostProjectSettings nocalhostProjectSettings = project.getService(NocalhostProjectSettings.class);

                    ServiceProjectPath devModeService = nocalhostProjectSettings.getDevModeService();
                    if (devModeService == null || devModeService.getRawKubeConfig() == null) {
                        return null;
                    }

                    Path kubeConfigPath = KubeConfigUtil.kubeConfigPath(devModeService.getRawKubeConfig());

                    try {
                        NhctlDescribeOptions nhctlDescribeOptions = new NhctlDescribeOptions(kubeConfigPath,
                                devModeService.getNamespace());
                        nhctlDescribeOptions.setDeployment(devModeService.getServiceName());
                        nhctlDescribeOptions.setType(devModeService.getServiceType());
                        NhctlDescribeService nhctlDescribeService = nhctlCommand.describe(
                                devModeService.getApplicationName(),
                                nhctlDescribeOptions,
                                NhctlDescribeService.class
                        );
                        if (nhctlDescribeService.isDeveloping()) {
                            if (!nhctlDescribeService.isPossess()) {
                                return null;
                            }
                        } else {
                            return null;
                        }

                        NhctlSyncStatusOptions nhctlSyncStatusOptions = new NhctlSyncStatusOptions(kubeConfigPath, devModeService.getNamespace());
                        nhctlSyncStatusOptions.setDeployment(devModeService.getServiceName());
                        nhctlCommand.syncStatusOverride(devModeService.getApplicationName(), nhctlSyncStatusOptions);
                    } catch (InterruptedException | NocalhostExecuteCmdException | IOException e) {
                        LOG.error("error occurred while sync status override ", e);
                    }
                    break;
                }
                case Messages.NO: {
                    break;
                }
                default:
            }
        }
        return null;
    }

    @Override
    public @Nullable String getSelectedValue() {
        nhctlSyncStatus = getNhctlSyncStatus();
        if (nhctlSyncStatus != null) {
            return "Nocalhost Sync Status: " + getNhctlSyncStatus().getMsg();
        }
        return "";
    }

    @Override
    public @Nullable Icon getIcon() {
        if (nhctlSyncStatus == null) {
            return null;
        }
        if (StringUtils.isNoneBlank(nhctlSyncStatus.getOutOfSync())) {
            return AllIcons.Toolwindows.ToolWindowProblemsEmpty;
        }
        String status = nhctlSyncStatus.getStatus();
        switch (status) {
            case "outOfSync":
                return AllIcons.General.Warning;
            case "disconnected":
                return AllIcons.Nodes.Pluginnotinstalled;
            case "error":
                return AllIcons.Actions.Cancel;
            case "scanning":
            case "syncthing":
                return AllIcons.Actions.Refresh;
            case "idle":
                return AllIcons.Actions.Checked;
            case "end":
                break;
            default:
                break;
        }
        return null;
    }
}
