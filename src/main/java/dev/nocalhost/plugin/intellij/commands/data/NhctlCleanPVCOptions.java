package dev.nocalhost.plugin.intellij.commands.data;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NhctlCleanPVCOptions extends NhctlGlobalOptions {
    private String app;
    private String svc;
    private String name;
}
