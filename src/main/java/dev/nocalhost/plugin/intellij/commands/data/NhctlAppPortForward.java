package dev.nocalhost.plugin.intellij.commands.data;

import com.google.gson.annotations.SerializedName;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NhctlAppPortForward {
    @SerializedName("svcName")
    private String serviceName;

    @SerializedName("servicetype")
    private String serviceType;

    private String port;

    private String status;

    private String role;

    private boolean sudo;

    @SerializedName("daemonserverpid")
    private int daemonServerPid;
}
