package dev.nocalhost.plugin.intellij.ui.action.application;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

import org.jetbrains.annotations.NotNull;

import dev.nocalhost.plugin.intellij.api.data.Application;
import dev.nocalhost.plugin.intellij.api.data.DevSpace;
import dev.nocalhost.plugin.intellij.commands.NhctlCommand;
import dev.nocalhost.plugin.intellij.commands.data.NhctlDescribeOptions;
import dev.nocalhost.plugin.intellij.ui.tree.node.ApplicationNode;
import dev.nocalhost.plugin.intellij.ui.vfs.ReadOnlyVirtualFile;
import lombok.SneakyThrows;

public class LoadResourceAction extends AnAction {
    private static final Logger LOG = Logger.getInstance(LoadResourceAction.class);

    private final Project project;
    private final ApplicationNode node;

    public LoadResourceAction(Project project, ApplicationNode node) {
        super("Load Resource");
        this.project = project;
        this.node = node;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        final DevSpace devSpace = node.getDevSpace();
        final Application application = node.getApplication();

        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Loading kubernetes resources") {
            private VirtualFile virtualFile;

            @Override
            public void onSuccess() {
                FileEditorManager.getInstance(project).openTextEditor(new OpenFileDescriptor(project, virtualFile, 0), true);
            }

            @Override
            public void onThrowable(@NotNull Throwable e) {
                LOG.error("error occurred while describing application", e);
            }

            @SneakyThrows
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                final NhctlCommand nhctlCommand = ServiceManager.getService(NhctlCommand.class);

                NhctlDescribeOptions opts = new NhctlDescribeOptions(devSpace);
                String resource = nhctlCommand.describe(application.getContext().getApplicationName(), opts);
                String filename = application.getContext().getApplicationName() + ".yaml";
                virtualFile = new ReadOnlyVirtualFile(filename, filename, resource);
            }
        });

    }
}
