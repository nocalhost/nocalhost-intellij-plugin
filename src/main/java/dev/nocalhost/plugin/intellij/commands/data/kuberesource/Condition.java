package dev.nocalhost.plugin.intellij.commands.data.kuberesource;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Condition {
    private String status;
    private String type;
}
