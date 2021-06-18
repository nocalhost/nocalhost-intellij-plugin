package dev.nocalhost.plugin.intellij.commands.data.kuberesource;

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class KubeResource {
    private String kind;
    private Metadata metadata;
    private Spec spec;
    private Status status;

    public boolean canSelector() {
        return getStatus() != null && getMetadata() != null
                && StringUtils.equalsIgnoreCase(getStatus().getPhase(), "running")
                && StringUtils.isBlank(getMetadata().getDeletionTimestamp());
    }
}
