package dev.nocalhost.plugin.intellij.commands;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.io.CharStreams;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessHandlerFactory;
import com.intellij.openapi.components.ServiceManager;
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
import java.util.stream.Collectors;

import dev.nocalhost.plugin.intellij.api.data.DevSpace;
import dev.nocalhost.plugin.intellij.commands.data.KubeResource;
import dev.nocalhost.plugin.intellij.commands.data.KubeResourceList;
import dev.nocalhost.plugin.intellij.exception.NocalhostExecuteCmdException;
import dev.nocalhost.plugin.intellij.settings.NocalhostSettings;
import dev.nocalhost.plugin.intellij.utils.DataUtils;
import dev.nocalhost.plugin.intellij.utils.KubeConfigUtil;

public class KubectlCommand {
    private static final String KUBECTL_COMMAND = "kubectl";

    public KubeResourceList getResourceList(String kind, Map<String, String> labels, DevSpace devSpace) throws IOException, InterruptedException, NocalhostExecuteCmdException {
        Path kubeconfigPath = KubeConfigUtil.kubeConfigPath(devSpace);

        List<String> args = Lists.newArrayList(getKubectlCmd(), "get", kind);
        args.add("-n");
        args.add(devSpace.getNamespace());
        args.add("-o");
        args.add("json");
        args.add("--kubeconfig");
        args.add(kubeconfigPath.toString());
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

    public KubeResource getResource(String kind, String name, DevSpace devSpace) throws IOException, InterruptedException, NocalhostExecuteCmdException {
        Path kubeconfigPath = KubeConfigUtil.kubeConfigPath(devSpace);

        List<String> args = Lists.newArrayList(getKubectlCmd(), "get", kind);
        if (StringUtils.isNotBlank(name)) {
            args.add(name);
        }
        args.add("-n");
        args.add(devSpace.getNamespace());
        args.add("-o");
        args.add("json");
        args.add("--kubeconfig");
        args.add(kubeconfigPath.toString());

        return DataUtils.GSON.fromJson(executeCmd(args), KubeResource.class);
    }

    public String getResourceYaml(String kind, String name, DevSpace devSpace) throws IOException, InterruptedException, NocalhostExecuteCmdException {
        Path kubeconfigPath = KubeConfigUtil.kubeConfigPath(devSpace);

        List<String> args = Lists.newArrayList(getKubectlCmd(), "get", kind, name);
        args.add("-n");
        args.add(devSpace.getNamespace());
        args.add("-o");
        args.add("yaml");
        args.add("--kubeconfig");
        args.add(kubeconfigPath.toString());

        String cmd = String.join(" ", args.toArray(new String[]{}));
        System.out.println("Execute command: " + cmd);

        Process process = new ProcessBuilder(args).start();
        String output = CharStreams.toString(new InputStreamReader(process.getInputStream()));
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new NocalhostExecuteCmdException(cmd, exitCode, CharStreams.toString(new InputStreamReader(process.getErrorStream(), Charsets.UTF_8)));
        }

        return output;
    }

    public String exec(String podName, String containerName, String command, DevSpace devSpace) throws IOException, InterruptedException, NocalhostExecuteCmdException {
        Path kubeconfigPath = KubeConfigUtil.kubeConfigPath(devSpace);

        List<String> args = Lists.newArrayList(getKubectlCmd(), "exec", podName);
        args.add("--container");
        args.add(containerName);
        args.add("--kubeconfig");
        args.add(kubeconfigPath.toString());
        args.add("--");
        args.add(command);

        return executeCmd(args);
    }

    public String logs(String podName, String containerName, DevSpace devSpace) throws IOException, InterruptedException, NocalhostExecuteCmdException {
        Path kubeconfigPath = KubeConfigUtil.kubeConfigPath(devSpace);

        List<String> args = Lists.newArrayList(getKubectlCmd(), "logs", podName);
        args.add("--tail=200");
        args.add("--container");
        args.add(containerName);
        args.add("--kubeconfig");
        args.add(kubeconfigPath.toString());

        String output = executeCmd(args);
        System.out.println(output);

        return output;
    }

    public ProcessHandler getLogsProcessHandler(String podName, String containerName, DevSpace devSpace) throws ExecutionException {
        Path kubeconfigPath = KubeConfigUtil.kubeConfigPath(devSpace);

        List<String> args = Lists.newArrayList(getKubectlCmd(), "logs", podName);
        args.add("--follow=true");
        args.add("--tail=200");
        args.add("--container");
        args.add(containerName);
        args.add("--kubeconfig");
        args.add(kubeconfigPath.toString());

        String cmd = String.join(" ", args.toArray(new String[]{}));
        GeneralCommandLine commandLine = getCommandline(args);
        System.out.println("Execute command: " + cmd);

        return ProcessHandlerFactory.getInstance().createProcessHandler(commandLine);
    }

    public void apply(Path path, DevSpace devSpace) throws InterruptedException, NocalhostExecuteCmdException, IOException {
        Path kubeconfigPath = KubeConfigUtil.kubeConfigPath(devSpace);

        List<String> args = Lists.newArrayList(getKubectlCmd(), "apply");
        if (Files.isDirectory(path)) {
            args.add("--kustomize");
        } else {
            args.add("--filename");
        }
        args.add(path.toString());
        args.add("--kubeconfig");
        args.add(kubeconfigPath.toString());

        executeCmd(args);
    }

    private String executeCmd(List<String> args) throws IOException, InterruptedException, NocalhostExecuteCmdException {

        String cmd = String.join(" ", args.toArray(new String[]{}));
        GeneralCommandLine commandLine = getCommandline(args);
        Process process;
        try {
            process = commandLine.createProcess();
        } catch (ExecutionException e) {
            throw new NocalhostExecuteCmdException(cmd, -1, e.getMessage());
        }
        System.out.println("Execute command: " + cmd);

        String output = CharStreams.toString(new InputStreamReader(process.getInputStream(), Charsets.UTF_8));

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new NocalhostExecuteCmdException(cmd, exitCode, CharStreams.toString(new InputStreamReader(process.getErrorStream(), Charsets.UTF_8)));
        }
        return output;
    }

    protected String getKubectlCmd() {
        final NocalhostSettings nocalhostSettings = ServiceManager.getService(NocalhostSettings.class);
        String kubectlCmd = KUBECTL_COMMAND;
        if (StringUtils.isNoneBlank(nocalhostSettings.getKubectlBinary())) {
            kubectlCmd = nocalhostSettings.getKubectlBinary();
        }
        return kubectlCmd;
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
        return new GeneralCommandLine(args).withEnvironment(environment).withRedirectErrorStream(true);
    }
}
