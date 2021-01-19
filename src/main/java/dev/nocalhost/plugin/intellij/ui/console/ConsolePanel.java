package dev.nocalhost.plugin.intellij.ui.console;

import com.intellij.openapi.project.Project;

import javax.swing.*;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class ConsolePanel {
    protected String title;
    protected Icon icon;
    protected JComponent panel;
    protected Project project;

    public ConsolePanel(String title, Icon icon, Project project) {
        this.title = title;
        this.icon = icon;
        this.project = project;
    }
}
