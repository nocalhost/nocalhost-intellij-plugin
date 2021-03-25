package dev.nocalhost.plugin.intellij.commands.data;

public enum KubeResourceType {
    Deployment("Deployment"),
    Daemonset("Daemonset"),
    Statefulset("Statefulset"),
    Job("Job"),
    CronJobs("CronJobs"),
    Pod("Pod");

    private final String val;

    KubeResourceType(String val) {
        this.val = val;
    }

    public String getVal() {
        return val;
    }
}
