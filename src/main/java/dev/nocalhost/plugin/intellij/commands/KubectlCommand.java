package dev.nocalhost.plugin.intellij.commands;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.io.CharStreams;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessHandlerFactory;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.util.EnvironmentUtil;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import dev.nocalhost.plugin.intellij.commands.data.KubeResource;
import dev.nocalhost.plugin.intellij.commands.data.KubeResourceList;
import dev.nocalhost.plugin.intellij.exception.NocalhostExecuteCmdException;
import dev.nocalhost.plugin.intellij.utils.DataUtils;

public class KubectlCommand {
    private static final String KUBECTL_COMMAND = "kubectl";

    public KubeResourceList getResourceList(String kind, Map<String, String> labels, Path kubeConfigPath, String namespace) throws IOException, InterruptedException, NocalhostExecuteCmdException {
        List<String> args = Lists.newArrayList(getKubectlCmd(), "get", kind);
        args.add("-n");
        args.add(namespace);
        args.add("-o");
        args.add("json");
        args.add("--kubeconfig");
        args.add(kubeConfigPath.toString());
        if (labels != null) {
            args.add("--selector");
            args.add(labels.entrySet().stream()
                    .map((e) -> e.getKey() + "=" + e.getValue())
                    .collect(Collectors.joining(","))
            );
        }

        String output = executeCmd(args);
        return DataUtils.GSON.fromJson(output, KubeResourceList.class);
    }

    public KubeResourceList getNamespaceList(Path kubeConfigPath) throws IOException, InterruptedException, NocalhostExecuteCmdException {
        List<String> args = Lists.newArrayList(getKubectlCmd(), "get", "namespace");
        args.add("-o");
        args.add("json");
        args.add("--kubeconfig");
        args.add(kubeConfigPath.toString());

        String output = executeCmd(args);
        return DataUtils.GSON.fromJson(output, KubeResourceList.class);
    }

    public KubeResource getResource(String kind, String name, Path kubeConfigPath, String namespace) throws IOException, InterruptedException, NocalhostExecuteCmdException {
        List<String> args = Lists.newArrayList(getKubectlCmd(), "get", kind);
        if (StringUtils.isNotBlank(name)) {
            args.add(name);
        }
        args.add("-n");
        args.add(namespace);
        args.add("-o");
        args.add("json");
        args.add("--kubeconfig");
        args.add(kubeConfigPath.toString());

        return DataUtils.GSON.fromJson(executeCmd(args), KubeResource.class);
    }

    public String getResourceYaml(String kind, String name, Path kubeConfigPath, String namespace) throws IOException, InterruptedException, NocalhostExecuteCmdException {
        List<String> args = Lists.newArrayList(getKubectlCmd(), "get", kind, name);
        args.add("-n");
        args.add(namespace);
        args.add("-o");
        args.add("yaml");
        args.add("--kubeconfig");
        args.add(kubeConfigPath.toString());

        return executeCmd(args);
    }

    public ProcessHandler getLogsProcessHandler(String podName, String containerName, Path kubeConfigPath, String namespace) throws ExecutionException {
        List<String> args = Lists.newArrayList(getKubectlCmd(), "logs", podName);
        args.add("--follow=true");
        args.add("--tail=200");
        args.add("--container");
        args.add(containerName);
        args.add("--kubeconfig");
        args.add(kubeConfigPath.toString());
        args.add("--namespace");
        args.add(namespace);

        String cmd = String.join(" ", args.toArray(new String[]{}));
        GeneralCommandLine commandLine = getCommandline(args);
        System.out.println("Execute command: " + cmd);

        return ProcessHandlerFactory.getInstance().createProcessHandler(commandLine);
    }

    private String executeCmd(List<String> args) throws IOException, InterruptedException, NocalhostExecuteCmdException {
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
            throw new NocalhostExecuteCmdException(cmd, exitCode, CharStreams.toString(new InputStreamReader(process.getErrorStream(), Charsets.UTF_8)));
        }
        return output;
    }

    protected String getKubectlCmd() {
        return KUBECTL_COMMAND;
    }

    protected GeneralCommandLine getCommandline(List<String> args) {
        final Map<String, String> environment = new HashMap<>(EnvironmentUtil.getEnvironmentMap());
        environment.put("DISABLE_SPINNER", "true");
        if (SystemInfo.isMac || SystemInfo.isLinux) {
            String path = environment.get("PATH");
            String kubectlCmd = getKubectlCmd();
            if (StringUtils.contains(kubectlCmd, "/")) {
                path = kubectlCmd.substring(0, kubectlCmd.lastIndexOf("/")) + ":" + path;
                environment.put("PATH", path);
            }
        }
        return new GeneralCommandLine(args).withEnvironment(environment);
    }
}
