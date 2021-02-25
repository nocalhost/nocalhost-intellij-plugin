package dev.nocalhost.plugin.intellij.ui.console;

import com.intellij.ui.content.Content;

import javax.swing.*;

public abstract class NocalhostConsoleWindow {

    public abstract JComponent getPanel();

    public abstract String getTitle();
}
