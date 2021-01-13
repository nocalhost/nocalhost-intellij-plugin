package dev.nocalhost.plugin.intellij.ui.tree;

import dev.nocalhost.plugin.intellij.api.data.DevSpace;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class WorkloadNode {
    private String name;
    private Status status;
    private DevSpace devSpace;

    public enum Status {
        RUNNING, UNKNOWN, STARTING
    }
}
