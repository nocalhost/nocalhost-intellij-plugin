package dev.nocalhost.plugin.intellij.commands;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.io.CharStreams;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.util.EnvironmentUtil;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dev.nocalhost.plugin.intellij.exception.NocalhostExecuteCmdException;

public class GitCommand {
    private static final String GIT_COMMAND = "git";

    public void clone(Path parentDir, String url, String dir) throws InterruptedException, NocalhostExecuteCmdException, IOException {
        List<String> args = Lists.newArrayList(
                GIT_COMMAND,
                "clone",
                url,
                parentDir.resolve(dir).toAbsolutePath().toString());

        execute(args);
    }

    protected String execute(List<String> args) throws IOException, InterruptedException, NocalhostExecuteCmdException {
        GeneralCommandLine commandLine = getCommandline(args);
        String cmd = commandLine.getCommandLineString();
        Process process;
        try {
            process = commandLine.createProcess();
        } catch (ExecutionException e) {
            throw new NocalhostExecuteCmdException(cmd, -1, e.getMessage());
        }

        String output = CharStreams.toString(new InputStreamReader(process.getInputStream(), Charsets.UTF_8));

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new NocalhostExecuteCmdException(cmd, exitCode, output);
        }
        return output;
    }

    protected GeneralCommandLine getCommandline(List<String> args) {
        final Map<String, String> environment = new HashMap<>(EnvironmentUtil.getEnvironmentMap());
        environment.put("DISABLE_SPINNER", "true");
        if (SystemInfo.isMac || SystemInfo.isLinux) {
            String path = environment.get("PATH");
            if (StringUtils.contains(GIT_COMMAND, "/")) {
                path = GIT_COMMAND.substring(0, GIT_COMMAND.lastIndexOf("/")) + ":" + path;
                environment.put("PATH", path);
            }
        }
        return new GeneralCommandLine(args).withEnvironment(environment).withRedirectErrorStream(true);
    }
}
