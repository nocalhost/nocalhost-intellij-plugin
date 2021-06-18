package dev.nocalhost.plugin.intellij.commands.data.kuberesource;

import java.util.Map;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Metadata {
    private String name;
    private Map<String, String> labels;
    private String deletionTimestamp;
    private Map<String, String> annotations;
}
