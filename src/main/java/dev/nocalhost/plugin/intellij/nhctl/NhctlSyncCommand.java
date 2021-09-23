package dev.nocalhost.plugin.intellij.nhctl;

import com.google.common.collect.Lists;

import org.apache.commons.lang3.StringUtils;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NhctlSyncCommand extends BaseCommand {
    private boolean override;
    private String deployment;
    private String controllerType;
    private String applicationName;

    @Override
    protected List<String> compute() {
        List<String> args = Lists.newArrayList(getBinaryPath(), "sync-status", applicationName);
        if (StringUtils.isNotEmpty(namespace)) {
            args.add("--namespace");
            args.add(namespace);
        }
        if (StringUtils.isNotEmpty(deployment)) {
            args.add("--deployment");
            args.add(deployment);
        }
        if (StringUtils.isNotEmpty(controllerType)) {
            args.add("--controller-type");
            args.add(controllerType);
        }
        if (kubeConfig != null) {
            args.add("--kubeconfig");
            args.add(kubeConfig.toString());
        }
        if (override) {
            args.add("--override");
        }
        return args;
    }
}
