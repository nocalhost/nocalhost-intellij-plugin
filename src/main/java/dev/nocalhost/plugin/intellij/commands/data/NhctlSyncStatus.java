package dev.nocalhost.plugin.intellij.commands.data;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NhctlSyncStatus {
    private String status;
    private String msg;
    private String tips;
    private String outOfSync;
}
