package dev.nocalhost.plugin.intellij.commands;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.io.CharStreams;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.stream.Collectors;

import dev.nocalhost.plugin.intellij.commands.data.NhctlDevEndOptions;
import dev.nocalhost.plugin.intellij.commands.data.NhctlDevStartOptions;
import dev.nocalhost.plugin.intellij.commands.data.NhctlGlobalOptions;
import dev.nocalhost.plugin.intellij.commands.data.NhctlInstallOptions;
import dev.nocalhost.plugin.intellij.commands.data.NhctlPortForwardOptions;
import dev.nocalhost.plugin.intellij.commands.data.NhctlSyncOptions;
import dev.nocalhost.plugin.intellij.commands.data.NhctlUninstallOptions;


public final class NhctlCommand {
    private static final String NHCTL_COMMAND = "/usr/local/bin/nhctl";

    public void install(String name, NhctlInstallOptions opts) throws IOException, InterruptedException {
        List<String> args = Lists.newArrayList(NHCTL_COMMAND, "install", name);
        if (StringUtils.isNotEmpty(opts.getConfig())) {
            args.add("--config");
            args.add(opts.getConfig());
        }
        if (StringUtils.isNotEmpty(opts.getGitRef())) {
            args.add("--git-ref");
            args.add(opts.getGitRef());
        }
        if (StringUtils.isNotEmpty(opts.getGitUrl())) {
            args.add("--git-url");
            args.add(opts.getGitUrl());
        }
        if (StringUtils.isNotEmpty(opts.getHelmChartName())) {
            args.add("--helm-chart-name");
            args.add(opts.getHelmChartName());
        }
        if (StringUtils.isNotEmpty(opts.getHelmRepoName())) {
            args.add("--helm-repo-name");
            args.add(opts.getHelmRepoName());
        }
        if (StringUtils.isNotEmpty(opts.getHelmRepoUrl())) {
            args.add("--helm-repo-url");
            args.add(opts.getHelmRepoUrl());
        }
        if (StringUtils.isNotEmpty(opts.getHelmRepoVersion())) {
            args.add("--helm-repo-version");
            args.add(opts.getHelmRepoVersion());
        }
        if (StringUtils.isNotEmpty(opts.getHelmValues())) {
            args.add("--helm-values");
            args.add(opts.getHelmValues());
        }
        if (opts.isIgnorePreInstall()) {
            args.add("--ignore-pre-install");
        }
        if (StringUtils.isNotEmpty(opts.getNamespace())) {
            args.add("--namespace");
            args.add(opts.getNamespace());
        }
        if (StringUtils.isNotEmpty(opts.getOuterConfig())) {
            args.add("--outer-config");
            args.add(opts.getOuterConfig());
        }
        if (opts.getResourcesPath() != null) {
            for (String path : opts.getResourcesPath()) {
                args.add("--resource-path");
                args.add(path);
            }
        }
        if (opts.getValues() != null) {
            String values = opts.getValues().entrySet()
                    .stream()
                    .map((e) -> e.getKey() + "=" + e.getValue())
                    .collect(Collectors.toList())
                    .stream()
                    .reduce(",", String::concat);
            if (StringUtils.isNotEmpty(values)) {
                args.add("--set");
                args.add(values);
            }
        }
        if (StringUtils.isNotEmpty(opts.getType())) {
            args.add("--type");
            args.add(opts.getType());
        }
        if (opts.isWait()) {
            args.add("--wait");
        }

        execute(args, opts);
    }

    public void uninstall(String name, NhctlUninstallOptions opts) throws InterruptedException, IOException {
        List<String> args = Lists.newArrayList(NHCTL_COMMAND, "uninstall", name);
        if (opts.isForce()) {
            args.add("--force");
        }

        execute(args, opts);
    }

    public void devStart(String name, NhctlDevStartOptions opts) throws IOException, InterruptedException {
        List<String> args = Lists.newArrayList(NHCTL_COMMAND, "dev", "start", name);
        if (StringUtils.isNotEmpty(opts.getDeployment())) {
            args.add("--deployment");
            args.add(opts.getDeployment());
        }
        if (StringUtils.isNotEmpty(opts.getImage())) {
            args.add("--image");
            args.add(opts.getImage());
        }
        if (opts.getLocalSync() != null) {
            for (String s : opts.getLocalSync()) {
                args.add("--local-sync");
                args.add(s);
            }
        }
        if (StringUtils.isNotEmpty(opts.getSidecarImage())) {
            args.add("--sidecar-image");
            args.add(opts.getSidecarImage());
        }
        if (StringUtils.isNotEmpty(opts.getStorageClass())) {
            args.add("--storage-class");
            args.add(opts.getStorageClass());
        }
        if (StringUtils.isNotEmpty(opts.getSyncthingVersion())) {
            args.add("--syncthing-version");
            args.add(opts.getSyncthingVersion());
        }
        if (StringUtils.isNotEmpty(opts.getWorkDir())) {
            args.add("--work-dir");
            args.add(opts.getWorkDir());
        }

        execute(args, opts);
    }

    public void devEnd(String name, NhctlDevEndOptions opts) throws IOException, InterruptedException {
        List<String> args = Lists.newArrayList(NHCTL_COMMAND, "dev", "end", name);
        if (StringUtils.isNotEmpty(opts.getDeployment())) {
            args.add("--deployment");
            args.add(opts.getDeployment());
        }

        execute(args, opts);
    }

    public void sync(String name, NhctlSyncOptions opts) throws IOException, InterruptedException {
        List<String> args = Lists.newArrayList(NHCTL_COMMAND, "sync", name);
        if (opts.isDaemon()) {
            args.add("--daemon");
        }
        if (StringUtils.isNotEmpty(opts.getDeployment())) {
            args.add("--deployment");
            args.add(opts.getDeployment());
        }
        if (opts.isDoubleSideSync()) {
            args.add("--double");
        }
        if (opts.getIgnoredPatterns() != null) {
            for (String pattern : opts.getIgnoredPatterns()) {
                args.add("--ignored-pattern");
                args.add(pattern);
            }
        }
        if (opts.getSyncedPatterns() != null) {
            for (String pattern : opts.getSyncedPatterns()) {
                args.add("--synced-pattern");
                args.add(pattern);
            }
        }

        execute(args, opts);
    }

    public void portForward(String name, NhctlPortForwardOptions opts) throws IOException, InterruptedException {
        List<String> args = Lists.newArrayList(NHCTL_COMMAND, "port-forward", name);
        if (opts.isDaemon()) {
            args.add("--daemon");
        }
        if (StringUtils.isNotEmpty(opts.getDeployment())) {
            args.add("--deployment");
            args.add(opts.getDeployment());
        }
        if (opts.getDevPorts() != null) {
            for (String port : opts.getDevPorts()) {
                args.add("--dev-port");
                args.add(port);
            }
        }

        execute(args, opts);
    }

    private void execute(List<String> args, NhctlGlobalOptions opts) throws IOException, InterruptedException {
        addGlobalOptions(args, opts);

        String cmd = String.join(" ", args.toArray(new String[]{}));
        System.out.println("Execute command: " + cmd);

        Process process = Runtime.getRuntime().exec(cmd);
        if (process.waitFor() != 0) {
            System.out.println(CharStreams.toString(new InputStreamReader(
                    process.getInputStream(), Charsets.UTF_8)));
            System.err.println(CharStreams.toString(new InputStreamReader(
                    process.getErrorStream(), Charsets.UTF_8)));
            throw new RuntimeException(CharStreams.toString(new InputStreamReader(
                    process.getErrorStream(), Charsets.UTF_8)));
        }
    }

    private void addGlobalOptions(List<String> args, NhctlGlobalOptions opts) {
        if (opts.isDebug()) {
            args.add("--debug");
        }
        if (StringUtils.isNotEmpty(opts.getKubeconfig())) {
            args.add("--kubeconfig");
            args.add(opts.getKubeconfig());
        }
    }
}
