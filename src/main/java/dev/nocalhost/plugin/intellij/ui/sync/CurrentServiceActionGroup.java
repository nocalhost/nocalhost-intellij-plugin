package dev.nocalhost.plugin.intellij.ui.sync;

import com.intellij.dvcs.ui.PopupElementWithAdditionalInfo;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;;

import javax.swing.*;

import dev.nocalhost.plugin.intellij.commands.data.NhctlDevAssociateQueryResult;
import dev.nocalhost.plugin.intellij.utils.NhctlUtil;
import lombok.Setter;

public class CurrentServiceActionGroup extends ActionGroup implements PopupElementWithAdditionalInfo {
    private final Project project;

    @Setter
    private NhctlDevAssociateQueryResult result;

    public CurrentServiceActionGroup(@NotNull Project project, @NotNull NhctlDevAssociateQueryResult result, @Nullable Icon icon) {
        super(getTitle(result), true);
        this.result = result;
        this.project = project;
        var presentation = getTemplatePresentation();
        presentation.setIcon(icon);
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
        var service = NhctlUtil.getDevModeService(project);
        service

        if (StringUtils.equals("end", result.getSyncthingStatus().getStatus())) {
            return new AnAction[] {
                    new SwitchToCurrentAction(project),
                    new DeAssociateAction(project)
            };
        }
        return new AnAction[] {
                new ResumeSyncAction(project),
                new OverrideSyncAction(project)
        };
    }

    @Override
    public @Nls @Nullable String getInfoText() {
        return result.getSyncthingStatus().getMessage();
    }
}
