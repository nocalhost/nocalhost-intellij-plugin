package dev.nocalhost.plugin.intellij.ui.sync;

import com.intellij.dvcs.ui.PopupElementWithAdditionalInfo;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class CurrentServiceActionGroup extends ActionGroup implements PopupElementWithAdditionalInfo {
    private final String desc;
    private final Project project;

    public CurrentServiceActionGroup(@NotNull Project project, @NotNull String text, @Nullable String desc, @Nullable Icon icon) {
        super(text, true);
        this.desc = desc;
        this.project = project;
        var presentation = getTemplatePresentation();
        presentation.setIcon(icon);
    }

    @Override
    public AnAction @NotNull [] getChildren(@Nullable AnActionEvent e) {
        return new AnAction[]{
                new OverrideSyncAction(project),
                new ResumeSyncAction(project)
        };
    }

    @Override
    public @Nls @Nullable String getInfoText() {
        return desc;
    }
}
