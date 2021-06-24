package dev.nocalhost.plugin.intellij.utils;

import org.apache.commons.compress.utils.Lists;

import java.util.List;
import java.util.stream.Collectors;

import dev.nocalhost.plugin.intellij.commands.data.kuberesource.Container;
import dev.nocalhost.plugin.intellij.commands.data.kuberesource.KubeResource;

import static dev.nocalhost.plugin.intellij.utils.Constants.WORKLOAD_TYPE_CRONJOB;
import static dev.nocalhost.plugin.intellij.utils.Constants.WORKLOAD_TYPE_DAEMONSET;
import static dev.nocalhost.plugin.intellij.utils.Constants.WORKLOAD_TYPE_DEPLOYMENT;
import static dev.nocalhost.plugin.intellij.utils.Constants.WORKLOAD_TYPE_JOB;
import static dev.nocalhost.plugin.intellij.utils.Constants.WORKLOAD_TYPE_POD;
import static dev.nocalhost.plugin.intellij.utils.Constants.WORKLOAD_TYPE_STATEFULSET;

public final class KubeResourceUtil {
    public static List<String> resolveContainers(KubeResource resource) {
        switch (resource.getKind().toLowerCase()) {
            case WORKLOAD_TYPE_DEPLOYMENT:
            case WORKLOAD_TYPE_DAEMONSET:
            case WORKLOAD_TYPE_STATEFULSET:
            case WORKLOAD_TYPE_JOB:
                return resource
                        .getSpec()
                        .getTemplate()
                        .getSpec()
                        .getContainers()
                        .stream()
                        .map(Container::getName)
                        .collect(Collectors.toList());
            case WORKLOAD_TYPE_CRONJOB:
                return resource
                        .getSpec()
                        .getJobTemplate()
                        .getSpec()
                        .getTemplate()
                        .getSpec()
                        .getContainers()
                        .stream()
                        .map(Container::getName)
                        .collect(Collectors.toList());
            case WORKLOAD_TYPE_POD:
                return resource
                        .getSpec()
                        .getContainers()
                        .stream()
                        .map(Container::getName)
                        .collect(Collectors.toList());
            default:
                return Lists.newArrayList();
        }
    }
}
