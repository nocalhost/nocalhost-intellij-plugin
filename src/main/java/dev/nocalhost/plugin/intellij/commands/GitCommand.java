package dev.nocalhost.plugin.intellij.commands;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.io.CharStreams;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.List;

import dev.nocalhost.plugin.intellij.exception.NocalhostExecuteCmdException;

public class GitCommand {
    private static final String GIT_COMMAND = "git";

    public void clone(Path parentDir, String url, String clonedDirectoryName) throws IOException, InterruptedException, NocalhostExecuteCmdException {
        List<String> args = Lists.newArrayList(GIT_COMMAND, "clone");
        args.add("--progress");
        args.add(url);
        args.add(clonedDirectoryName);

        execute(args, parentDir.toString());
    }

    public String remote(String path) throws IOException, InterruptedException, NocalhostExecuteCmdException {
        List<String> args = Lists.newArrayList(GIT_COMMAND, "remote", "-v");
        return execute(args, path);
    }

    protected String execute(List<String> args, String directory) throws IOException, InterruptedException, NocalhostExecuteCmdException {
        String cmd = String.join(" ", args.toArray(new String[]{}));
        System.out.println("Execute command: " + cmd);

        Process process = new ProcessBuilder(args)
                .redirectErrorStream(true)
                .directory(new File(directory))
                .start();
        String output = CharStreams.toString(new InputStreamReader(process.getInputStream(), Charsets.UTF_8));
        int exitCode = process.waitFor();
        if (process.waitFor() != 0) {
            throw new NocalhostExecuteCmdException(cmd, exitCode, output);
        }
        return output;
    }
}
