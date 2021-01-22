package dev.nocalhost.plugin.intellij.commands.data;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NhctlDescribeService {
    private String actualName;
    private boolean developing;
    private boolean portForwarded;
    private boolean syncing;
}
