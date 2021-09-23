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
import dev.nocalhost.plugin.intellij.utils.NhctlUtil;
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
                return AllIcons.Actions.Refresh;
            case "error":
                return AllIcons.General.Error;
            case "idle":
                return AllIcons.Actions.Commit;
            case "end":
                return NocalhostIcons.Status.Normal;
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

    public @NotNull String getSha() {
        return result.getSha();
    }

    @Override
    public AnAction @NotNull [] getChildren(@Nullable AnActionEvent e) {
        List<AnAction> actions = Lists.newArrayList();
        var status = result.getSyncthingStatus().getStatus();
        var service = NhctlUtil.getDevModeService(project);

        if (service == null || !StringUtils.equals(service.getSha(), result.getSha())) {
            actions.add(new SwitchToCurrentAction(project));
        }

        switch (status) {
            case "disconnected":
                actions.add(new ResumeSyncAction(project));
                break;
            case "error":
                break;
            case "end":
                actions.add(new DeAssociateAction(project));
                break;
            default:
                actions.add(new OverrideSyncAction(project));
                break;
        }
        if ( ! actions.isEmpty()) {
            actions.add(0, new Separator("[Cluster]"));
        }
        return actions.toArray(new AnAction[0]);
    }

    @Override
    public @Nls @Nullable String getInfoText() {
        return result.getSyncthingStatus().getMessage();
    }
}
