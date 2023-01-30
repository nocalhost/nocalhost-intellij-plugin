package dev.nocalhost.plugin.intellij.ui;

import com.google.common.collect.Lists;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import com.intellij.dvcs.ui.LightActionGroup;
import com.intellij.icons.AllIcons;
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
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import javax.swing.*;

import dev.nocalhost.plugin.intellij.commands.NhctlCommand;
import dev.nocalhost.plugin.intellij.commands.data.NhctlDevAssociateQueryResult;
import dev.nocalhost.plugin.intellij.commands.data.NhctlSyncStatus;
import dev.nocalhost.plugin.intellij.commands.data.NhctlSyncStatusOptions;
import dev.nocalhost.plugin.intellij.exception.NocalhostExecuteCmdException;
import dev.nocalhost.plugin.intellij.nhctl.NhctlAssociateQueryerCommand;
import dev.nocalhost.plugin.intellij.service.NocalhostContextManager;
import dev.nocalhost.plugin.intellij.topic.NocalhostSyncUpdateNotifier;
import dev.nocalhost.plugin.intellij.ui.sync.ServiceActionGroup;
import dev.nocalhost.plugin.intellij.ui.sync.NocalhostSyncPopup;
import dev.nocalhost.plugin.intellij.utils.DataUtils;
import icons.NocalhostIcons;

public class SyncStatusPresentation implements StatusBarWidget.MultipleTextValuesPresentation, StatusBarWidget.Multiframe {

    private static final Logger LOG = Logger.getInstance(SyncStatusPresentation.class);

    private final StatusBar statusBar;
    private final Project project;
    private final Disposable widget;

    private final AtomicReference<NhctlSyncStatus> nhctlSyncStatus = new AtomicReference<>();
    private final AtomicReference<List<NhctlDevAssociateQueryResult>> services = new AtomicReference<>(Lists.newArrayList());

    private NhctlSyncStatus getNhctlSyncStatus() {
        if (project == null) {
            return null;
        }
        final NhctlCommand nhctlCommand = ApplicationManager.getApplication().getService(NhctlCommand.class);

        var context = NocalhostContextManager.getInstance(project).getContext();
        if (context == null) {
            return null;
        }

        try {
            NhctlSyncStatusOptions nhctlSyncStatusOptions = new NhctlSyncStatusOptions(context.getKubeConfigPath(), context.getNamespace());
            nhctlSyncStatusOptions.setDeployment(context.getServiceName());
            nhctlSyncStatusOptions.setControllerType(context.getServiceType());
            String status = nhctlCommand.syncStatus(context.getApplicationName(), nhctlSyncStatusOptions);
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

    public SyncStatusPresentation(Project project, StatusBar statusBar, Disposable widget) {
        this.widget = widget;
        this.project = project;
        this.statusBar = statusBar;

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            while ( ! project.isDisposed()) {
                try {
                    nhctlSyncStatus.set(getNhctlSyncStatus());
                    Thread.sleep(3000);
                } catch (Exception ex) {
                    LOG.error("Failed to get sync status", ex);
                }
            }
        });

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            var path = project.getBasePath();
            if (path == null) {
                return;
            }

            var token = TypeToken.getParameterized(List.class, NhctlDevAssociateQueryResult.class).getType();
            var command = new NhctlAssociateQueryerCommand(project);
            command.setLocalSync(Paths.get(path).toString());

            while ( ! project.isDisposed()) {
                var json = "";
                try {
                    json = command.execute();
                    List<NhctlDevAssociateQueryResult> results = DataUtils.GSON.fromJson(json, token);
                    services.set(results);
                    project
                            .getMessageBus()
                            .syncPublisher(NocalhostSyncUpdateNotifier.NOCALHOST_SYNC_UPDATE_NOTIFIER_TOPIC)
                            .action(results);

                    Thread.sleep(3000);
                } catch (Exception ex) {
                    LOG.error("Failed to refresh service list: [" + json + "]", ex);
                }
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
        var actions = new LightActionGroup();
        var results = services.get();
        var context = NocalhostContextManager.getInstance(project).getContext();

        if (context != null) {
            actions.addSeparator("Current Service");
            results
                    .stream()
                    .filter(x -> StringUtils.equals(x.getSha(), context.getSha()))
                    .findFirst()
                    .ifPresent(x -> actions.add(new ServiceActionGroup(project, x)));
            results = results
                    .stream()
                    .filter(x -> !StringUtils.equals(x.getSha(), context.getSha()))
                    .collect(Collectors.toList());
        }
        if ( ! results.isEmpty()) {
            actions.addSeparator("Related Service");
            results.forEach(x -> actions.add(new ServiceActionGroup(project, x)));
        }
        return actions;
    }

    @Override
    public @Nullable("null means the widget is unable to show the popup") ListPopup getPopupStep() {
        return NocalhostSyncPopup.getInstance(project, createActions()).asListPopup();
    }

    @Override
    public @Nullable String getSelectedValue() {
        if (nhctlSyncStatus.get() != null) {
            return " " + nhctlSyncStatus.get().getMsg();
        }
        return " Waiting for enter DevMode";
    }

    @Override
    public @Nullable Icon getIcon() {
        if (nhctlSyncStatus.get() == null) {
            return NocalhostIcons.ConfigurationLogo;
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
            case "syncing":
                return NocalhostIcons.CloudUpload;
            case "idle":
                return AllIcons.Actions.Commit;
            case "end":
                return AllIcons.Actions.Exit;
            default:
                break;
        }
        return null;
    }
}
