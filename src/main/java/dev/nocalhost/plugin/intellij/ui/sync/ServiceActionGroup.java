package dev.nocalhost.plugin.intellij.ui.sync;

import com.google.common.collect.Lists;

import com.intellij.dvcs.ui.PopupElementWithAdditionalInfo;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Separator;
import com.intellij.openapi.project.Project;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import javax.swing.*;

import dev.nocalhost.plugin.intellij.commands.data.NhctlDevAssociateQueryResult;
import dev.nocalhost.plugin.intellij.i18n.NocalhostI18n;
import dev.nocalhost.plugin.intellij.service.NocalhostContextManager;
import icons.NocalhostIcons;
import lombok.Setter;

public class ServiceActionGroup extends ActionGroup implements PopupElementWithAdditionalInfo {
    private final Project project;

    @Setter
    private NhctlDevAssociateQueryResult result;

    public ServiceActionGroup(@NotNull Project project, @NotNull NhctlDevAssociateQueryResult result) {
        super(getTitle(result), true);
        this.result = result;
        this.project = project;
        var presentation = getTemplatePresentation();
        presentation.setIcon(getIcon(result));
    }

    private static @Nullable Icon getIcon(@NotNull NhctlDevAssociateQueryResult result) {
        switch (result.getSyncthingStatus().getStatus()) {
            case "disconnected":
                return AllIcons.Nodes.Pluginnotinstalled;
            case "outOfSync":
                return AllIcons.General.Warning;
            case "scanning":
            case "syncing":
                return NocalhostIcons.CloudUpload;
            case "error":
                return AllIcons.General.Error;
            case "idle":
                return AllIcons.Actions.Commit;
            case "end":
                return AllIcons.Actions.Exit;
            default:
                break;
        }
        return null;
    }

    private static @NotNull String getTitle(@NotNull NhctlDevAssociateQueryResult result) {
        return String.join("/", new String[] {
                result.getServicePack().getNamespace(),
                result.getServicePack().getApplicationName(),
                result.getServicePack().getServiceType(),
                result.getServicePack().getServiceName()
        });
    }

    public boolean compare(@NotNull NhctlDevAssociateQueryResult other) {
        return StringUtils.equals(result.getSha(), other.getSha());
    }

    @Override
    public AnAction @NotNull [] getChildren(@Nullable AnActionEvent e) {
        List<AnAction> actions = Lists.newArrayList();
        var status = result.getSyncthingStatus().getStatus();
        var context = NocalhostContextManager.getInstance(project).getContext();

        actions.add(new Separator("[" + result.getServer() + "]"));
        if (context == null || !StringUtils.equals(context.getSha(), result.getSha())) {
            actions.add(new SwitchAsCurrentAction(project, result));
        }
        switch (status) {
            case "disconnected":
                actions.add(new ResumeSyncAction(project, result));
                break;
            case "end":
                actions.add(new DisassociateAction(project, result));
                break;
            case "error":
                actions.add(new ResumeSyncAction(project, result));
                actions.add(new OverrideSyncAction(project, result));
                break;
            default:
                actions.add(new OverrideSyncAction(project, result));
                break;
        }
        if (StringUtils.isNotEmpty(result.getSyncthingStatus().getGui())) {
            actions.add(new OpenDashboardAction(project, result));
        }
        return actions.toArray(new AnAction[0]);
    }

    @Override
    public @Nls @Nullable String getInfoText() {
        return translateMessage(result.getSyncthingStatus().getMessage());
    }

    private static String translateMessage(@Nullable String message) {
        if (StringUtils.isEmpty(message)) {
            return message;
        }
        switch (message) {
            case "DevMode Starting...":
                return NocalhostI18n.get("sync.msg.devModeStarting");
            case "Welcome to Nocalhost":
                return NocalhostI18n.get("sync.msg.welcome");
            case "Application not installed":
                return NocalhostI18n.get("sync.msg.appNotInstalled");
            case "Not in DevMode":
                return NocalhostI18n.get("sync.msg.notInDevMode");
            case "Other device is developing":
                return NocalhostI18n.get("sync.msg.otherDeviceDeveloping");
            case "No syncthing process found":
                return NocalhostI18n.get("sync.msg.noSyncthingProcess");
            case "Disconnected from sidecar":
                return NocalhostI18n.get("sync.msg.disconnectedFromSidecar");
            case "Disconnected":
                return NocalhostI18n.get("sync.msg.disconnected");
            case "Error":
                return NocalhostI18n.get("sync.msg.error");
            case "Scanning local changed...":
                return NocalhostI18n.get("sync.msg.scanning");
            default:
        }
        if (message.startsWith("Sync completed at: ")) {
            return NocalhostI18n.format("sync.msg.syncCompleted",
                    message.substring("Sync completed at: ".length()));
        }
        if (message.startsWith("Out of sync! : ")) {
            return NocalhostI18n.format("sync.msg.outOfSync",
                    message.substring("Out of sync! : ".length()));
        }
        if (message.startsWith("Upload to remote: ")) {
            return NocalhostI18n.format("sync.msg.uploadToRemote",
                    message.substring("Upload to remote: ".length()));
        }
        return message;
    }
}
