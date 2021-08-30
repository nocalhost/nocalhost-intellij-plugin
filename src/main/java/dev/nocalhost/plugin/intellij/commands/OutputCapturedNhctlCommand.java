package dev.nocalhost.plugin.intellij.commands;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;

import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import dev.nocalhost.plugin.intellij.commands.data.NhctlGlobalOptions;
import dev.nocalhost.plugin.intellij.exception.NhctlCommandException;
import dev.nocalhost.plugin.intellij.exception.NocalhostExecuteCmdException;
import dev.nocalhost.plugin.intellij.service.ProgressProcessManager;
import dev.nocalhost.plugin.intellij.topic.NocalhostOutputAppendNotifier;
import dev.nocalhost.plugin.intellij.ui.console.NocalhostConsoleManager;
import dev.nocalhost.plugin.intellij.utils.NhctlOutputUtil;
import dev.nocalhost.plugin.intellij.utils.SudoUtil;

public final class OutputCapturedNhctlCommand extends NhctlCommand {
    private final Project project;

    public OutputCapturedNhctlCommand(Project project) {
        this.project = project;
    }

    @Override
    protected String execute(List<String> args, NhctlGlobalOptions opts, String sudoPassword)
            throws IOException, InterruptedException, NocalhostExecuteCmdException {
        addGlobalOptions(args, opts);

        NocalhostConsoleManager.activateOutputWindow(project);

        NocalhostOutputAppendNotifier publisher = project.getMessageBus().syncPublisher(
                NocalhostOutputAppendNotifier.NOCALHOST_OUTPUT_APPEND_NOTIFIER_TOPIC);

        if (sudoPassword != null) {
            args = SudoUtil.toSudoCommand(args);
        }

        GeneralCommandLine commandLine = getCommandline(args);
        String cmd = commandLine.getCommandLineString();

        publisher.action("[cmd] " + cmd + System.lineSeparator());

        Process process;
        try {
            process = commandLine.createProcess();
            if (opts != null && opts.getTask() != null) {
                ApplicationManager.getApplication().getService(ProgressProcessManager.class)
                        .add(opts.getTask(), process);
            }
            if (sudoPassword != null) {
                SudoUtil.inputPassword(process, sudoPassword);
            }
        } catch (ExecutionException e) {
            throw new NocalhostExecuteCmdException(cmd, -1, e.getMessage());
        }

        final AtomicReference<String> errorOutput = new AtomicReference<>();
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            InputStreamReader reader = new InputStreamReader(process.getErrorStream(), Charsets.UTF_8);
            try {
                errorOutput.set(CharStreams.toString(reader));
            } catch (Exception ignore) {
            }
        });

        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                process.getInputStream(), Charsets.UTF_8))) {
            String line;
            String previousLine = "";
            while ((line = br.readLine()) != null) {
                if (StringUtils.equals(line, previousLine)) {
                    continue;
                }
                publisher.action(line + System.lineSeparator());
                sb.append(line).append(System.lineSeparator());
                NhctlOutputUtil.showMessageByCommandOutput(project, line);
                previousLine = line;
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            publisher.action(errorOutput.get() + System.lineSeparator());
            sb.append(errorOutput.get()).append(System.lineSeparator());
            throw new NhctlCommandException(cmd, exitCode, sb.toString(), errorOutput.get());
        }

        return sb.toString();
    }
}
