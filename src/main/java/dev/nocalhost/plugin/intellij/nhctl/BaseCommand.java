package dev.nocalhost.plugin.intellij.nhctl;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.OSProcessUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.util.EnvironmentUtil;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import dev.nocalhost.plugin.intellij.utils.SudoUtil;
import dev.nocalhost.plugin.intellij.utils.NhctlUtil;
import dev.nocalhost.plugin.intellij.utils.NhctlOutputUtil;
import dev.nocalhost.plugin.intellij.exception.NhctlCommandException;
import dev.nocalhost.plugin.intellij.topic.NocalhostOutputAppendNotifier;
import dev.nocalhost.plugin.intellij.exception.NocalhostExecuteCmdException;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class BaseCommand {
    protected Process process;
    protected boolean console;
    protected Project project;
    protected Path kubeConfig;
    protected String namespace;
    protected String deployment;

    protected AtomicBoolean silent = new AtomicBoolean(false);
    protected AtomicReference<String> stderr = new AtomicReference<>("");

    protected BaseCommand(Project project) {
        this(project, true);
    }

    protected BaseCommand(Project project, boolean console) {
        this.project = project;
        this.console = console;
    }

    protected List<String> fulfill(@NotNull List<String> args) {
        if (kubeConfig != null) {
            args.add("--kubeconfig");
            args.add(kubeConfig.toString());
        }
        if (StringUtils.isNotEmpty(namespace)) {
            args.add("--namespace");
            args.add(namespace);
        }
        if (StringUtils.isNotEmpty(deployment)) {
            args.add("--deployment");
            args.add(deployment);
        }
        return args;
    }

    protected String getBinaryPath() {
        return NhctlUtil.binaryPath();
    }

    protected GeneralCommandLine getCommandline(@NotNull List<String> args) {
        Map<String, String> environment = new HashMap<>(EnvironmentUtil.getEnvironmentMap());
        environment.put("DISABLE_SPINNER", "true");
        if (SystemInfo.isMac || SystemInfo.isLinux) {
            String path = environment.get("PATH");
            String binary = getBinaryPath();
            if (StringUtils.contains(binary, "/")) {
                path = binary.substring(0, binary.lastIndexOf("/")) + ":" + path;
                environment.put("PATH", path);
            }
        }
        return new GeneralCommandLine(args).withEnvironment(environment);
    }

    protected abstract List<String> compute();

    protected void onInput(@NotNull Process process) {
        // do nothing
    }

    protected void onError(@NotNull Process process) {
        try (var reader = new InputStreamReader(process.getErrorStream(), Charsets.UTF_8)) {
            stderr.set(CharStreams.toString(reader));
        } catch (IOException ex) {
            // ignore
        }
    }

    public String execute() throws IOException, NocalhostExecuteCmdException, InterruptedException {
        return doExecute(compute());
    }

    public String execute(String password) throws IOException, NocalhostExecuteCmdException, InterruptedException {
        return doExecute(compute(), password);
    }

    protected String doExecute(@NotNull List<String> args) throws IOException, InterruptedException, NocalhostExecuteCmdException {
        return doExecute(args, null);
    }

    protected String doExecute(@NotNull List<String> args, String sudoPassword) throws InterruptedException, NocalhostExecuteCmdException, IOException {
        if (sudoPassword != null) {
            args = SudoUtil.toSudoCommand(args);
        }

        GeneralCommandLine commandLine = getCommandline(args);
        String cmd = commandLine.getCommandLineString();
        print("[cmd] " + cmd);

        try {
            process = commandLine.createProcess();
            if (sudoPassword != null) {
                SudoUtil.inputPassword(process, sudoPassword);
            }
        } catch (ExecutionException e) {
            throw new NocalhostExecuteCmdException(cmd, -1, e.getMessage());
        }

        ApplicationManager.getApplication().executeOnPooledThread(() -> onError(process));
        ApplicationManager.getApplication().executeOnPooledThread(() -> onInput(process));

        var stdout = new StringBuilder();
        var reader = new InputStreamReader(process.getInputStream(), Charsets.UTF_8);
        try (var br = new BufferedReader(reader)) {
            String line;
            while ((line = br.readLine()) != null) {
                print(line);
                stdout.append(line);
                NhctlOutputUtil.showMessageByCommandOutput(project, line);
            }
        }

        int code = process.waitFor();
        if (code != 0) {
            print(stderr.get());
            if (silent.get()) {
                return "";
            }
            throw new NhctlCommandException(cmd, code, stdout.toString(), stderr.get());
        }
        return stdout.toString();
    }

    public void print(String text) {
        if (console) {
            project
                    .getMessageBus()
                    .syncPublisher(NocalhostOutputAppendNotifier.NOCALHOST_OUTPUT_APPEND_NOTIFIER_TOPIC)
                    .action(text + System.lineSeparator());
        }
    }

    public void destroy() {
        silent.compareAndSet(false, true);
        if (process != null) {
            OSProcessUtil.killProcessTree(process);
        }
    }
}
