package dev.nocalhost.plugin.intellij.nhctl;

import com.google.common.collect.Lists;

import com.intellij.openapi.project.Project;

import java.util.List;

public class NhctlDeleteKubeConfigCommand extends BaseCommand {

    public NhctlDeleteKubeConfigCommand(Project project) {
        super(project);
    }

    @Override
    protected List<String> compute() {
        List<String> args = Lists.newArrayList(getBinaryPath(), "kubeconfig", "remove");
        return fulfill(args);
    }
}
