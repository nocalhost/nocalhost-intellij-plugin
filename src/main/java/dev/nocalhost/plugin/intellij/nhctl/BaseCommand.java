package dev.nocalhost.plugin.intellij.nhctl;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.util.EnvironmentUtil;

import org.apache.commons.compress.utils.Lists;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import dev.nocalhost.plugin.intellij.utils.SudoUtil;
import dev.nocalhost.plugin.intellij.utils.NhctlUtil;
import dev.nocalhost.plugin.intellij.exception.NhctlCommandException;
import dev.nocalhost.plugin.intellij.exception.NocalhostExecuteCmdException;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class BaseCommand {
    protected Path kubeConfig;
    protected String namespace;
    protected String deployment;

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
        final Map<String, String> environment = new HashMap<>(EnvironmentUtil.getEnvironmentMap());
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

    public String execute() throws IOException, NocalhostExecuteCmdException, InterruptedException {
        return doExecute(compute());
    }

    protected String doExecute(@NotNull List<String> args) throws IOException, InterruptedException, NocalhostExecuteCmdException {
        return doExecute(args, null);
    }

    protected String doExecute(@NotNull List<String> args, String sudoPassword) throws IOException, InterruptedException, NocalhostExecuteCmdException {
        if (sudoPassword != null) {
            args = SudoUtil.toSudoCommand(args);
        }

        GeneralCommandLine commandLine = getCommandline(args);
        String cmd = commandLine.getCommandLineString();

        Process process;
        try {
            process = commandLine.createProcess();
            if (sudoPassword != null) {
                SudoUtil.inputPassword(process, sudoPassword);
            }
        } catch (ExecutionException e) {
            throw new NocalhostExecuteCmdException(cmd, -1, e.getMessage());
        }

        final AtomicReference<String> err = new AtomicReference<>("");
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            var reader = new InputStreamReader(process.getErrorStream(), Charsets.UTF_8);
            try {
                err.set(CharStreams.toString(reader));
            } catch (Exception ex) {
                // ignored
            }
        });

        try (var reader = new InputStreamReader(process.getInputStream(), Charsets.UTF_8)) {
            var output = CharStreams.toString(reader);
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                output += err.get();
                throw new NhctlCommandException(cmd, exitCode, output, err.get());
            }
            return output;
        }
    }
}
