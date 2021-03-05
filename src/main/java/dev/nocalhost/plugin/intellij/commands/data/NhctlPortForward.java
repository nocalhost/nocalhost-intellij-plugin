package dev.nocalhost.plugin.intellij.commands.data;

import org.apache.commons.lang.StringUtils;

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
        if (StringUtils.isNotEmpty(way)) {
            return String.format("%s:%s(%s)", localport, remoteport, way);
        } else {
            return portForwardStr();
        }
    }

    public String portForwardStr() {
        return String.format("%s:%s", localport, remoteport);
    }
}
