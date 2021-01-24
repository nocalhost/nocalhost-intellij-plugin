package dev.nocalhost.plugin.intellij.helpers;

import org.apache.commons.lang3.StringUtils;

import java.util.List;

import dev.nocalhost.plugin.intellij.commands.data.KubeResource;

public final class KubectlHelper {
    public static boolean isKubeResourceAvailable(KubeResource kubeResource) {
        List<KubeResource.Status.Condition> conditions = kubeResource.getStatus().getConditions();
        for (KubeResource.Status.Condition condition : conditions) {
            if (StringUtils.equals(condition.getType(), "Available")
                    && StringUtils.equals(condition.getStatus(), "True")) {
                return true;
            }
        }
        return false;
    }
}
