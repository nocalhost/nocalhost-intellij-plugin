package dev.nocalhost.plugin.intellij.commands.data;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NhctlPortForward {
    private String localport;
    private String remoteport;
    private String way;
    private String status;
    private String reson;
    private String updated;
    private Integer pid;

    public String portForward() {
        return String.format("%s:%s(%s)", localport, remoteport, way);
    }

    public String portForwardStr() {
        return String.format("%s:%s", localport, remoteport);
    }
}
