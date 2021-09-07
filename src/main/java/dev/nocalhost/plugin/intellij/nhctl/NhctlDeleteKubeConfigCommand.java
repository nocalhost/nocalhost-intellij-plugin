package dev.nocalhost.plugin.intellij.nhctl;

import com.google.common.collect.Lists;

import java.nio.file.Path;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NhctlDeleteKubeConfigCommand extends BaseCommand {
    private Path kubeConfig;

    @Override
    protected List<String> compute() {
        List<String> args = Lists.newArrayList(getBinaryPath(), "kubeconfig", "remove");
        if (kubeConfig != null) {
            args.add("--kubeconfig");
            args.add(kubeConfig.toString());
        }
        return args;
    }
}
