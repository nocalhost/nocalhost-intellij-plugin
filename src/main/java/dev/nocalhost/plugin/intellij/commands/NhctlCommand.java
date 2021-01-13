package dev.nocalhost.plugin.intellij.commands;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.io.CharStreams;

import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import dev.nocalhost.plugin.intellij.commands.data.NhctlGlobalOptions;
import dev.nocalhost.plugin.intellij.commands.data.NhctlInstallOptions;
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
        for (String path : opts.getResourcesPath()) {
            args.add("--resource-path");
            args.add(path);
        }

        Map<String, String> valuesMap = opts.getValues();
        if (valuesMap != null && !valuesMap.isEmpty()) {
            String values = valuesMap.entrySet()
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
        addGlobalOptions(args, opts);

        String cmd = String.join(" ", args.toArray(new String[]{}));
        System.out.println("Execute command: " + cmd);

        Process process = Runtime.getRuntime().exec(cmd);
        if (process.waitFor() != 0) {
            System.out.println(CharStreams.toString(new InputStreamReader(
                    process.getInputStream(), Charsets.UTF_8)));
            System.err.println(CharStreams.toString(new InputStreamReader(
                    process.getErrorStream(), Charsets.UTF_8)));
            throw new RuntimeException();
        }
    }

    public void uninstall(String name, NhctlUninstallOptions opts) throws InterruptedException, IOException {
        List<String> args = Lists.newArrayList(NHCTL_COMMAND, "uninstall", name);
        if (opts.isForce()) {
            args.add("--force");
        }
        addGlobalOptions(args, opts);

        String cmd = String.join(" ", args.toArray(new String[]{}));
        System.out.println("Execute command: " + cmd);

        Process process = Runtime.getRuntime().exec(cmd);
        if (process.waitFor() != 0) {
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
