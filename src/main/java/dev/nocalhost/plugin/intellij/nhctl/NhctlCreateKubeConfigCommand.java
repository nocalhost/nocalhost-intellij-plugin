package dev.nocalhost.plugin.intellij.nhctl;

import com.google.common.collect.Lists;

import com.intellij.openapi.project.Project;

import java.util.List;

public class NhctlCreateKubeConfigCommand extends BaseCommand {

    public NhctlCreateKubeConfigCommand(Project project) {
        super(project);
    }

    @Override
    protected List<String> compute() {
        List<String> args = Lists.newArrayList(getBinaryPath(), "kubeconfig", "add");
        return fulfill(args);
    }
}
