package dev.nocalhost.plugin.intellij.nhctl;

import com.google.common.collect.Lists;
import java.util.List;

public class NhctlCreateKubeConfigCommand extends BaseCommand {
    @Override
    protected List<String> compute() {
        List<String> args = Lists.newArrayList(getBinaryPath(), "kubeconfig", "add");
        if (kubeConfig != null) {
            args.add("--kubeconfig");
            args.add(kubeConfig.toString());
        }
        return args;
    }
}
