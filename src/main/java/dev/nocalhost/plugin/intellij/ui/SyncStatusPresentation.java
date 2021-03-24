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
import java.util.List;

import javax.swing.*;

import dev.nocalhost.plugin.intellij.api.data.DevSpace;
import dev.nocalhost.plugin.intellij.commands.NhctlCommand;
import dev.nocalhost.plugin.intellij.commands.data.AliveDeployment;
import dev.nocalhost.plugin.intellij.commands.data.NhctlSyncResumeOptions;
import dev.nocalhost.plugin.intellij.commands.data.NhctlSyncStatus;
import dev.nocalhost.plugin.intellij.commands.data.NhctlSyncStatusOptions;
import dev.nocalhost.plugin.intellij.exception.NocalhostExecuteCmdException;
import dev.nocalhost.plugin.intellij.helpers.UserDataKeyHelper;
import dev.nocalhost.plugin.intellij.utils.DataUtils;

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

        List<AliveDeployment> aliveDeployments = UserDataKeyHelper.findAliveDeploymentsByProject(project);
        if (aliveDeployments == null) {
            return null;
        }
        AliveDeployment aliveDeployment = aliveDeployments.get(0);
        DevSpace devSpace = aliveDeployment.getDevSpace();
        String deployment = aliveDeployment.getDeployment();
        NhctlSyncStatusOptions options = new NhctlSyncStatusOptions(devSpace);
        options.setDeployment(deployment);
        String status = null;
        try {
            status = nhctlCommand.syncStatus(aliveDeployment.getApplication().getContext().getApplicationName(), options);
        } catch (InterruptedException | NocalhostExecuteCmdException | IOException e) {
            LOG.error("error occurred while get sync status ", e);
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
                    List<AliveDeployment> aliveDeployments = UserDataKeyHelper.findAliveDeploymentsByProject(project);
                    if (aliveDeployments == null) {
                        return null;
                    }
                    AliveDeployment aliveDeployment = aliveDeployments.get(0);
                    DevSpace devSpace = aliveDeployment.getDevSpace();
                    String deployment = aliveDeployment.getDeployment();
                    NhctlSyncResumeOptions options = new NhctlSyncResumeOptions(devSpace);
                    options.setDeployment(deployment);
                    try {
                        nhctlCommand.syncResume(aliveDeployment.getApplication().getContext().getApplicationName(), options);
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

                    List<AliveDeployment> aliveDeployments = UserDataKeyHelper.findAliveDeploymentsByProject(project);
                    if (aliveDeployments == null) {
                        return null;
                    }
                    AliveDeployment aliveDeployment = aliveDeployments.get(0);
                    DevSpace devSpace = aliveDeployment.getDevSpace();
                    String deployment = aliveDeployment.getDeployment();
                    NhctlSyncStatusOptions options = new NhctlSyncStatusOptions(devSpace);
                    options.setDeployment(deployment);
                    try {
                        nhctlCommand.syncStatusOverride(aliveDeployment.getApplication().getContext().getApplicationName(), options);
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
