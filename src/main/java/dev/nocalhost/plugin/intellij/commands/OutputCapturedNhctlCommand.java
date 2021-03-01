package dev.nocalhost.plugin.intellij.commands;

import com.google.common.base.Charsets;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.serviceContainer.AlreadyDisposedException;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;

import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import dev.nocalhost.plugin.intellij.exception.NocalhostNotifier;
import dev.nocalhost.plugin.intellij.commands.data.NhctlGlobalOptions;
import dev.nocalhost.plugin.intellij.exception.NocalhostExecuteCmdException;
import dev.nocalhost.plugin.intellij.topic.NocalhostOutputAppendNotifier;

public final class OutputCapturedNhctlCommand extends NhctlCommand {
    private static final Logger LOG = Logger.getInstance(OutputCapturedNhctlCommand.class);

    private final Project project;

    public OutputCapturedNhctlCommand(Project project) {
        this.project = project;
    }

    @Override
    protected String execute(List<String> args, NhctlGlobalOptions opts) throws IOException, InterruptedException, NocalhostExecuteCmdException {
        addGlobalOptions(args, opts);

        activateOutputWindow();

        NocalhostOutputAppendNotifier publisher = project.getMessageBus()
                .syncPublisher(NocalhostOutputAppendNotifier.NOCALHOST_OUTPUT_APPEND_NOTIFIER_TOPIC);

        String cmd = String.join(" ", args.toArray(new String[]{}));
        publisher.action("[cmd] " + cmd + System.lineSeparator());

        GeneralCommandLine commandLine = getCommandline(args);
        Process process;
        try {
            process = commandLine.createProcess();
        } catch (ExecutionException e) {
            throw new NocalhostExecuteCmdException(cmd, -1, e.getMessage());
        }

        StringBuilder sb = new StringBuilder();
        BufferedReader br = new BufferedReader(new InputStreamReader(
                process.getInputStream(), Charsets.UTF_8));
        String line;
        String previousLine = "";
        while ((line = br.readLine()) != null) {
            if (StringUtils.equals(line, previousLine)) {
                continue;
            }
            publisher.action(line + System.lineSeparator());
            sb.append(line).append(System.lineSeparator());
            previousLine = line;
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new NocalhostExecuteCmdException(cmd, exitCode, sb.toString());
        }

        return sb.toString();
    }

    private void activateOutputWindow() {
        ApplicationManager.getApplication().invokeAndWait(() -> {
            try {
                ToolWindowManager.getInstance(project).getToolWindow("Nocalhost Console").activate(() -> {
                    ContentManager contentManager = ToolWindowManager.getInstance(project).getToolWindow("Nocalhost Console").getContentManager();
                    Content content = contentManager.getContent(0);
                    if (content != null) {
                        contentManager.setSelectedContent(content);
                    }
                });
            } catch (AlreadyDisposedException e) {
                // Ignore
            } catch (Exception e) {
                LOG.error("error occurred while activate output window", e);
                NocalhostNotifier.getInstance(project).notifyError("Nocalhost starting dev mode error", "Error occurred while starting dev mode", e.getMessage());
            }
        });
    }
}
