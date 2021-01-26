package dev.nocalhost.plugin.intellij.commands.data;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NhctlListPVCOptions extends NhctlGlobalOptions {
    private String app;
    private String svc;
}
