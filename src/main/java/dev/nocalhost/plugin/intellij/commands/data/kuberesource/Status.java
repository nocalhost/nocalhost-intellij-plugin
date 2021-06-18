package dev.nocalhost.plugin.intellij.commands.data.kuberesource;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Status {
    private List<Condition> conditions;
    private int readyReplicas;
    private int replicas;
    private String phase;
    private int numberReady;
    private int numberAvailable;
}
