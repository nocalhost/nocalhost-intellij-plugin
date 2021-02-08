package dev.nocalhost.plugin.intellij.helpers;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.util.Pair;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import dev.nocalhost.plugin.intellij.commands.KubectlCommand;
import dev.nocalhost.plugin.intellij.commands.data.KubeResource;
import dev.nocalhost.plugin.intellij.ui.tree.node.ResourceNode;
import dev.nocalhost.plugin.intellij.utils.DataUtils;

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

    public static Pair<String, String> getResourceYaml(ResourceNode resourceNode) throws IOException, InterruptedException {
        final KubectlCommand kubectlCommand = ServiceManager.getService(KubectlCommand.class);

        String resourceYaml = kubectlCommand.getResourceYaml(
                resourceNode.getKubeResource().getKind(),
                resourceNode.getKubeResource().getMetadata().getName(),
                resourceNode.devSpace());

        Map m = DataUtils.YAML.load(resourceYaml);
        return Pair.create((String) m.get("kind"), resourceYaml);
    }

}
