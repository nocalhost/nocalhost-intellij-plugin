package dev.nocalhost.plugin.intellij.commands;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.io.CharStreams;
import com.google.gson.Gson;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import dev.nocalhost.plugin.intellij.api.data.DevSpace;
import dev.nocalhost.plugin.intellij.commands.data.KubeResource;
import dev.nocalhost.plugin.intellij.commands.data.KubeResourceList;
import dev.nocalhost.plugin.intellij.utils.KubeConfigUtil;

public class KubectlCommand {
    private static final String KUBECTL_COMMAND = "/usr/local/bin/kubectl";

    private final Gson gson = new Gson();

    public KubeResourceList getResourceList(String kind, Map<String, String> labels, DevSpace devSpace) throws IOException, InterruptedException {
        Path kubeconfigPath = KubeConfigUtil.kubeConfigPath(devSpace);
        ensureKubeconfigExisted(kubeconfigPath, devSpace.getKubeConfig());

        List<String> args = Lists.newArrayList(KUBECTL_COMMAND, "get", kind);
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

        String cmd = String.join(" ", args.toArray(new String[]{}));
        System.out.println("Execute command: " + cmd);

        Process process = Runtime.getRuntime().exec(cmd);
        if (process.waitFor() != 0) {
            throw new RuntimeException(CharStreams.toString(new InputStreamReader(
                    process.getErrorStream(), Charsets.UTF_8)));
        }

        return gson.fromJson(new InputStreamReader(process.getInputStream(), Charsets.UTF_8), KubeResourceList.class);
    }

    public KubeResource getResource(String kind, String name, DevSpace devSpace) throws IOException, InterruptedException {
        Path kubeconfigPath = KubeConfigUtil.kubeConfigPath(devSpace);
        ensureKubeconfigExisted(kubeconfigPath, devSpace.getKubeConfig());

        List<String> args = Lists.newArrayList(KUBECTL_COMMAND, "get", kind, name);
        args.add("-n");
        args.add(devSpace.getNamespace());
        args.add("-o");
        args.add("json");
        args.add("--kubeconfig");
        args.add(kubeconfigPath.toString());

        String cmd = String.join(" ", args.toArray(new String[]{}));
        System.out.println("Execute command: " + cmd);

        Process process = Runtime.getRuntime().exec(cmd);
        if (process.waitFor() != 0) {
            throw new RuntimeException(CharStreams.toString(new InputStreamReader(
                    process.getErrorStream(), Charsets.UTF_8)));
        }

        return gson.fromJson(new InputStreamReader(process.getInputStream(), Charsets.UTF_8), KubeResource.class);
    }

    public String exec(String podName, String containerName, String command, DevSpace devSpace) throws IOException, InterruptedException {
        Path kubeconfigPath = KubeConfigUtil.kubeConfigPath(devSpace);
        ensureKubeconfigExisted(kubeconfigPath, devSpace.getKubeConfig());

        List<String> args = Lists.newArrayList(KUBECTL_COMMAND, "exec", podName);
        args.add("--container");
        args.add(containerName);
        args.add("--");
        args.add(command);
        args.add("--kubeconfig");
        args.add(kubeconfigPath.toString());

        String cmd = String.join(" ", args.toArray(new String[]{}));
        System.out.println("Execute command: " + cmd);

        Process process = Runtime.getRuntime().exec(cmd);
        if (process.waitFor() != 0) {
            throw new RuntimeException(CharStreams.toString(new InputStreamReader(
                    process.getErrorStream(), Charsets.UTF_8)));
        }

        return CharStreams.toString(new InputStreamReader(process.getInputStream(), Charsets.UTF_8));
    }

    private void ensureKubeconfigExisted(Path path, String content) throws IOException {
        if (Files.exists(path)) {
            return;
        }
        Files.createDirectories(path.getParent());
        Files.write(path, content.getBytes(StandardCharsets.UTF_8));
    }
}
