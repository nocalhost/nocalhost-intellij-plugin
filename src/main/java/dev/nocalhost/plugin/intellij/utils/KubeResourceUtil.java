package dev.nocalhost.plugin.intellij.utils;

import org.apache.commons.compress.utils.Lists;

import java.util.List;
import java.util.stream.Collectors;

import dev.nocalhost.plugin.intellij.commands.data.kuberesource.Container;
import dev.nocalhost.plugin.intellij.commands.data.kuberesource.KubeResource;

public final class KubeResourceUtil {
    public static List<String> resolveContainers(KubeResource resource) {
        switch (resource.getKind().toLowerCase()) {
            case "deployment":
            case "daemonset":
            case "statefuleset":
            case "job":
                return resource
                        .getSpec()
                        .getTemplate()
                        .getSpec()
                        .getContainers()
                        .stream()
                        .map(Container::getName)
                        .collect(Collectors.toList());
            case "cronjob":
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
            case "pod":
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
