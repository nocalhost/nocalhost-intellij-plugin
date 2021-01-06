package dev.nocalhost.plugin.intellij.ui;

import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.ContentManager;

import org.jetbrains.annotations.NotNull;

import dev.nocalhost.plugin.intellij.NocalhostModule;

public class NocalhostWindowFactory implements ToolWindowFactory, DumbAware {
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        NocalhostWindow nocalhostWindow = NocalhostModule.getInstance(NocalhostWindow.class);

        SimpleToolWindowPanel toolWindowPanel = nocalhostWindow.createToolWindowContent(project);

        ContentManager contentManager = toolWindow.getContentManager();
        Content content = ContentFactory.SERVICE.getInstance().createContent(toolWindowPanel, "", false);
        contentManager.addContent(content);
        contentManager.setSelectedContent(content);
    }
}
