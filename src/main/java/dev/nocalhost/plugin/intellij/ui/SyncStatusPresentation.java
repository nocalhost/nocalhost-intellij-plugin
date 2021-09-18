package dev.nocalhost.plugin.intellij.ui;

import com.google.gson.JsonSyntaxException;

import com.intellij.dvcs.ui.LightActionGroup;
import com.intellij.icons.AllIcons;
import com.intellij.ide.DataManager;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
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
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.*;

import dev.nocalhost.plugin.intellij.commands.NhctlCommand;
import dev.nocalhost.plugin.intellij.commands.data.NhctlSyncStatus;
import dev.nocalhost.plugin.intellij.commands.data.NhctlSyncStatusOptions;
import dev.nocalhost.plugin.intellij.data.ServiceProjectPath;
import dev.nocalhost.plugin.intellij.exception.NocalhostExecuteCmdException;
import dev.nocalhost.plugin.intellij.service.NocalhostProjectService;
import dev.nocalhost.plugin.intellij.ui.sync.CurrentServiceActionGroup;
import dev.nocalhost.plugin.intellij.ui.sync.NocalhostSyncPopup;
import dev.nocalhost.plugin.intellij.utils.DataUtils;

public class SyncStatusPresentation implements StatusBarWidget.MultipleTextValuesPresentation, StatusBarWidget.Multiframe {

    private static final Logger LOG = Logger.getInstance(SyncStatusPresentation.class);

    private final StatusBar statusBar;
    private final Project project;
    private final Disposable widget;
    private final NocalhostProjectService nocalhostProjectService;

    private final AtomicReference<NhctlSyncStatus> nhctlSyncStatus = new AtomicReference<>();

    private NhctlSyncStatus getNhctlSyncStatus() {
        if (project == null) {
            return null;
        }
        final NhctlCommand nhctlCommand = ApplicationManager.getApplication().getService(NhctlCommand.class);

        ServiceProjectPath devModeService = nocalhostProjectService.getServiceProjectPath();
        if (devModeService == null) {
            return null;
        }

        try {
            NhctlSyncStatusOptions nhctlSyncStatusOptions = new NhctlSyncStatusOptions(devModeService.getKubeConfigPath(), devModeService.getNamespace());
            nhctlSyncStatusOptions.setDeployment(devModeService.getServiceName());
            nhctlSyncStatusOptions.setControllerType(devModeService.getServiceType());
            String status = nhctlCommand.syncStatus(devModeService.getApplicationName(), nhctlSyncStatusOptions);
            return DataUtils.GSON.fromJson(status, NhctlSyncStatus.class);
        } catch (InterruptedException | IOException e) {
            LOG.error("error occurred while get sync status ", e);
        } catch (NocalhostExecuteCmdException e) {
            if (!StringUtils.contains(e.getMessage(), "not found")) {
                LOG.error("Fail to get sync status", e);
            }
        } catch (JsonSyntaxException ignore) {
        }
        return null;
    }

    public SyncStatusPresentation(StatusBar statusBar, Project project, Disposable widget) {
        this.statusBar = statusBar;
        this.project = project;
        this.widget = widget;
        nocalhostProjectService = project.getService(NocalhostProjectService.class);

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                while (!project.isDisposed()) {
                    nhctlSyncStatus.set(getNhctlSyncStatus());
                    Thread.sleep(3000);
                }
            } catch (Exception e) {
                LOG.error("Fail to get sync status", e);
            }
        });
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
        if (nhctlSyncStatus.get() != null) {
            if (StringUtils.isNoneBlank(nhctlSyncStatus.get().getOutOfSync())) {
                return nhctlSyncStatus.get().getOutOfSync();
            }
            return nhctlSyncStatus.get().getTips();
        }
        return "";
    }

    @Override
    public @Nullable Consumer<MouseEvent> getClickConsumer() {
        return mouseEvent -> {
            statusBar.updateWidget("Nocalhost Sync Status");
        };
    }

    @NotNull
    private ActionGroup createActions() {
        LightActionGroup popupGroup = new LightActionGroup();
        popupGroup.addSeparator("Current Service");
        popupGroup.add(new CurrentServiceActionGroup(project, "nh3zxjm/bookinfo/deployment/productpage", "Sync completed at 18:32:06", AllIcons.Actions.Commit));
        popupGroup.addSeparator("Related Service");
        popupGroup.add(new CurrentServiceActionGroup(project, "nh3zxjm/bookinfo/deployment/authors", "Upload to remote 22.3%", AllIcons.Actions.Refresh));
        popupGroup.add(new CurrentServiceActionGroup(project, "nh3zxjm/bookinfo/deployment/reviews", "Disconnected from sidecar", AllIcons.Debugger.ThreadStates.Socket));
        return popupGroup;
    }

    @Override
    public @Nullable("null means the widget is unable to show the popup") ListPopup getPopupStep() {
        return new NocalhostSyncPopup(
                "Nocalhost Sync Manage",
                createActions(),
                DataManager.getInstance().getDataContext(statusBar.getComponent()));

//        return new BranchActionGroupPopup("Nocalhost Sync", project, (action) -> false, createActions(), "Nocalhost.Sync");

//
//        final OutputCapturedNhctlCommand outputCapturedNhctlCommand = project.getService(OutputCapturedNhctlCommand.class);
//
//        if (nhctlSyncStatus.get() != null && nhctlSyncStatus.get().getStatus().equalsIgnoreCase("disconnected")) {
//            if (MessageDialogBuilder.yesNo("Sync Resume", "do you want to resume file sync?").ask(project)) {
//                ServiceProjectPath devModeService = nocalhostProjectService.getServiceProjectPath();
//                if (devModeService == null) {
//                    return null;
//                }
//
//                ApplicationManager.getApplication().executeOnPooledThread(() -> {
//                    try {
//                        NhctlSyncOptions nhctlSyncOptions = new NhctlSyncOptions(devModeService.getKubeConfigPath(), devModeService.getNamespace());
//                        nhctlSyncOptions.setDeployment(devModeService.getServiceName());
//                        nhctlSyncOptions.setControllerType(devModeService.getServiceType());
//                        nhctlSyncOptions.setContainer(devModeService.getContainerName());
//                        nhctlSyncOptions.setResume(true);
//                        outputCapturedNhctlCommand.sync(devModeService.getApplicationName(), nhctlSyncOptions);
//                    } catch (InterruptedException | NocalhostExecuteCmdException | IOException e) {
//                        LOG.error("error occurred while sync resume ", e);
//                    }
//                });
//            }
//        }
//        if (nhctlSyncStatus.get() != null && StringUtils.isNoneBlank(nhctlSyncStatus.get().getOutOfSync())) {
//            if (MessageDialogBuilder.yesNo("Sync Override", "Override the remote changes according to the local folders?").ask(project)) {
//                ServiceProjectPath devModeService = nocalhostProjectService.getServiceProjectPath();
//                if (devModeService == null) {
//                    return null;
//                }
//
//
//                ApplicationManager.getApplication().executeOnPooledThread(() -> {
//                    try {
//                        NhctlSyncStatusOptions nhctlSyncStatusOptions = new NhctlSyncStatusOptions(devModeService.getKubeConfigPath(), devModeService.getNamespace());
//                        nhctlSyncStatusOptions.setDeployment(devModeService.getServiceName());
//                        nhctlSyncStatusOptions.setControllerType(devModeService.getServiceType());
//                        nhctlSyncStatusOptions.setOverride(true);
//                        outputCapturedNhctlCommand.syncStatus(devModeService.getApplicationName(), nhctlSyncStatusOptions);
//                    } catch (InterruptedException | NocalhostExecuteCmdException | IOException e) {
//                        LOG.error("error occurred while sync status override ", e);
//                    }
//                });
//            }
//        }
//        return null;
    }

    @Override
    public @Nullable String getSelectedValue() {
        if (nhctlSyncStatus.get() != null) {
            return "Nocalhost Sync Status: " + nhctlSyncStatus.get().getMsg();
        }
        return "";
    }

    @Override
    public @Nullable Icon getIcon() {
        if (nhctlSyncStatus.get() == null) {
            return null;
        }
        if (StringUtils.isNoneBlank(nhctlSyncStatus.get().getOutOfSync())) {
            return AllIcons.Toolwindows.ToolWindowProblemsEmpty;
        }
        String status = nhctlSyncStatus.get().getStatus();
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
