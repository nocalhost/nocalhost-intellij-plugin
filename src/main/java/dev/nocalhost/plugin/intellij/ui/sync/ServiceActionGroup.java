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
import org.jetbrains.annotations.Nullable;;

import java.util.List;

import javax.swing.*;

import dev.nocalhost.plugin.intellij.commands.data.NhctlDevAssociateQueryResult;
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
        return result.getSyncthingStatus().getMessage();
    }
}
