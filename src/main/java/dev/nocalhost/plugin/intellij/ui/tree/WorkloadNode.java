package dev.nocalhost.plugin.intellij.ui.tree;

import lombok.Getter;

@Getter
public class WorkloadNode {
    private String name;
    private Status status;

    public WorkloadNode(String name, Status status) {
        this.name = name;
        this.status = status;
    }
    public static enum Status {
        RUNNING, UNKNOWN, STARTING
    }
}
