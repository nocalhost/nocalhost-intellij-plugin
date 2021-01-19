package dev.nocalhost.plugin.intellij.ui.console;

import com.intellij.openapi.project.Project;

import java.awt.*;

import javax.swing.*;

public class LogPanel extends ConsolePanel {

    public LogPanel(String title, Icon icon, Project project) {
        super(title, icon, project);

        panel = new JPanel(new GridLayout(1, 1));
    }
}
