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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dev.nocalhost.plugin.intellij.exception.NocalhostExecuteCmdException;
import dev.nocalhost.plugin.intellij.exception.NocalhostGitCloneException;

public class GitCommand {
    private static final String GIT_COMMAND = "git";
    private static final Pattern HEAD_BRANCH_PATTERN = Pattern.compile("HEAD branch\\:\\s(.+)");

    public void clone(Path gitDir, String url) throws NocalhostExecuteCmdException, InterruptedException, IOException {
        clone(gitDir, url, null);
    }

    public void clone(Path gitDir, String url, String ref) throws IOException, NocalhostExecuteCmdException, InterruptedException {
        Files.createDirectories(gitDir);

        String workTreePath = gitDir.toAbsolutePath().toString();
        String gitDirPath = gitDir.resolve(".git").toAbsolutePath().toString();

        List<String> init = Lists.newArrayList(GIT_COMMAND, "--work-tree", workTreePath, "--git-dir", gitDirPath,
                "init", ".");
        execute(init);

        List<String> addRemote = Lists.newArrayList(GIT_COMMAND, "--work-tree", workTreePath, "--git-dir", gitDirPath,
                "remote", "add", "origin", url);
        execute(addRemote);

        List<String> configSshCommand = Lists.newArrayList(GIT_COMMAND, "--work-tree", workTreePath, "--git-dir", gitDirPath,
                "config", "core.sshCommand", "ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no");
        execute(configSshCommand);

        List<String> fetch = Lists.newArrayList(GIT_COMMAND, "--work-tree", workTreePath, "--git-dir", gitDirPath,
                "fetch", "--prune", "--depth", "1", "origin");
        execute(fetch);

        if (!StringUtils.isNotEmpty(ref)) {
            List<String> getHeadBranch = Lists.newArrayList(GIT_COMMAND, "--work-tree", workTreePath, "--git-dir", gitDirPath,
                    "remote", "show", "origin");
            String output = execute(getHeadBranch);
            Matcher matcher = HEAD_BRANCH_PATTERN.matcher(output);
            if (!matcher.find()) {
                throw new NocalhostGitCloneException("No head branch found from repository " + url);
            }
            ref = matcher.group(1);
        }

        List<String> checkout = Lists.newArrayList(GIT_COMMAND, "--work-tree", workTreePath, "--git-dir", gitDirPath,
                "checkout", ref);
        execute(checkout);
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

        try (InputStreamReader reader = new InputStreamReader(process.getInputStream(), Charsets.UTF_8)) {
            String output = CharStreams.toString(reader);
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new NocalhostExecuteCmdException(cmd, exitCode, output);
            }
            return output;
        }
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
